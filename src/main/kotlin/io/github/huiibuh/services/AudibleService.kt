package io.github.huiibuh.services

import audible.client.AudibleClient
import io.github.huiibuh.config.Settings

object AudibleService : AudibleClient(Settings.audibleSearchHost, Settings.audibleAuthorHost)
