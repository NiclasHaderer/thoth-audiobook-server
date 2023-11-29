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
