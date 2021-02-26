package push

import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECPublicKeySpec
import java.security.*
import java.security.spec.InvalidKeySpecException
import java.util.*

data class PushSubscription(
    val endpoint: String,
    val keys: Keys
) {

    init {
        // Add BouncyCastle as an algorithm provider
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }

    @Throws(NoSuchAlgorithmException::class, InvalidKeySpecException::class, NoSuchProviderException::class)
    fun getUserPublicKey(): PublicKey? {
        val kf: KeyFactory = KeyFactory.getInstance("ECDH", BouncyCastleProvider.PROVIDER_NAME)
        val ecSpec = ECNamedCurveTable.getParameterSpec("secp256r1")
        val point = ecSpec.curve.decodePoint(keys.keyAsBytes)
        val pubSpec = ECPublicKeySpec(point, ecSpec)
        return kf.generatePublic(pubSpec)
    }
}

data class Keys(val auth: String, val p256dh: String) {
    val keyAsBytes = Base64.getUrlDecoder().decode(p256dh)!!
    val authAsBytes = Base64.getUrlDecoder().decode(auth)!!
}
