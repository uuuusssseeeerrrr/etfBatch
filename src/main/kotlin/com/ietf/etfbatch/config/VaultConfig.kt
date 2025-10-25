package com.ietf.etfbatch.config

import com.oracle.bmc.ConfigFileReader
import com.oracle.bmc.auth.AuthenticationDetailsProvider
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider
import com.oracle.bmc.secrets.SecretsClient
import com.oracle.bmc.secrets.model.Base64SecretBundleContentDetails
import com.oracle.bmc.secrets.requests.GetSecretBundleRequest
import kotlin.io.encoding.Base64

object VaultConfig {
    private val configFile = ConfigFileReader.parseDefault()
    private val provider: AuthenticationDetailsProvider = ConfigFileAuthenticationDetailsProvider(configFile)
    private val client = SecretsClient.builder()
        .endpoint("https://secrets.vaults.ap-chuncheon-1.oci.oraclecloud.com")
        .build(provider)

    fun getVaultSecret(vaultKey: String): String {
        val getSecretRequest = GetSecretBundleRequest.builder()
            .secretId(configFile.get(vaultKey))
            .build()

        val decoder =
            client.getSecretBundle(getSecretRequest).secretBundle.secretBundleContent as Base64SecretBundleContentDetails

        return String(Base64.decode(decoder.content))
    }
}

