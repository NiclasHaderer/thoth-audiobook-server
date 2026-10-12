/* eslint-disable */
export type ApiError = {
    success: false,
    error: string | object
    status?: number
}

export type ApiSuccess<T> = {
    success: true,
    body: T
}

export type ApiResponse<T> = ApiError | ApiSuccess<T>

export type ApiCallData = {
    requiresAuth: boolean,
    route: string,
    method: string,
    body?: object,
    headers: Headers,
    bodySerializer: (body?: object) => string | undefined,
    executor: (callData: ApiCallData) => Promise<Response | ApiResponse<any>>,
}

export type ApiInterceptor = (param: ApiCallData) => ApiCallData | Promise<ApiCallData>

type ArrayIsch<T> = T | T[]
export const _createUrl = (route: string, params: Record<string, ArrayIsch<string | number | boolean | undefined | null>>): string => {
    const finalUrlParams = new URLSearchParams()
    // noinspection TypeScriptUnresolvedReference
    for (let [key, value] of Object.entries(params)) {
        if (Array.isArray(value)) {
            const newValue = value.filter(i => i !== "" && i !== undefined && i !== null) as (string | number | boolean)[]
            newValue.forEach(v => finalUrlParams.append(key, v.toString()))
        } else {
            if (value !== null && value !== undefined) {
                finalUrlParams.append(key, value.toString())
            }
        }
    }
    return finalUrlParams ? `${route}?${finalUrlParams.toString()}` : route
};

export const _request = async <T>(
    route: string,
    method: string,
    bodyParseMethod: "text" | "json" | "blob",
    headers: Headers,
    body: object | undefined,
    interceptors: ApiInterceptor[],
    executor: ApiCallData["executor"],
    requiresAuth: boolean,
): Promise<ApiResponse<T>> => {
    if (!headers.has("Content-Type")) {
        headers.set("Content-Type", "application/json");
    }
    let apiCallData: ApiCallData = {
        route,
        method,
        body,
        headers,
        bodySerializer: (body?: object) => body ? JSON.stringify(body) : undefined,
        executor,
        requiresAuth,
    };
    if (interceptors.length > 0) {
        for (const interceptor of interceptors) {
            apiCallData = await interceptor(apiCallData) as ApiCallData;
        }
    }

    return apiCallData.executor(apiCallData)
        .then(async (response) => {
            if (!(response instanceof Response)) {
                return response;
            }

            if (response.ok) {
                return {
                    success: true,
                    body: await response[bodyParseMethod]()
                } as const
            } else {
                return {
                    success: false,
                    status: response.status,
                    error: await response.json().catch(() => response.text()).catch(() => response.statusText)
                } as const
            }
        })
        .catch((error) => ({success: false, error: error?.toString()} as const))
};

export const _mergeHeaders = (headers: Headers, newHeaders: HeadersInit): Headers => {
    const mergedHeaders = new Headers(headers)
    for (const [key, value] of Object.entries(newHeaders)) {
        mergedHeaders.set(key, value)
    }
    return mergedHeaders
}

export type ApiSocket<SEND, RECEIVE> = {
    socket: WebSocket,
    send: (message: SEND) => void,
    onMessage: (listener: (message: RECEIVE) => void) => () => void,
    close: () => void,
}

export type ApiSocketData = {
    requiresAuth: boolean,
    route: string,
    messageSerializer: (message: unknown) => string,
    executor: (socketData: ApiSocketData) => WebSocket,
}

export type ApiSocketInterceptor = (param: ApiSocketData) => ApiSocketData | Promise<ApiSocketData>

export const _socket = async <SEND, RECEIVE>(
    route: string,
    interceptors: ApiSocketInterceptor[],
    executor: ApiSocketData["executor"],
    requiresAuth: boolean,
): Promise<ApiSocket<SEND, RECEIVE>> => {
    let socketData: ApiSocketData = {
        route,
        messageSerializer: (message: unknown) => JSON.stringify(message),
        executor,
        requiresAuth,
    };
    for (const interceptor of interceptors) {
        socketData = await interceptor(socketData);
    }

    const socket = socketData.executor(socketData)
    return {
        socket,
        send: (message) => socket.send(socketData.messageSerializer(message)),
        onMessage: (listener) => {
            const handler = (event: MessageEvent) => listener(JSON.parse(event.data))
            socket.addEventListener("message", handler)
            return () => socket.removeEventListener("message", handler)
        },
        close: () => socket.close(),
    }
}