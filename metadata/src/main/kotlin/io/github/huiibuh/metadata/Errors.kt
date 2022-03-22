package io.github.huiibuh.metadata

class ProviderNotFoundException(providerID: ProviderWithIDMetadata) :
    Exception("Provider with id ${providerID.provider} was not found")
