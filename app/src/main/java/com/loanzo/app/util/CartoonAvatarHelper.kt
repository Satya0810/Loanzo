package com.loanzo.app.util

import com.loanzo.app.R
import com.loanzo.app.data.entity.MarketplacePostEntity
import com.loanzo.app.data.entity.UserEntity
import java.net.URLEncoder
import kotlin.math.abs

/**
 * Utility to generate high-quality 2D cartoon person avatars.
 * Provides deterministic online PNG URLs (DiceBear Personas) with pastel backgrounds
 * and seamless offline local 2D vector drawables (ic_avatar_cartoon_1 to 6).
 */
object CartoonAvatarHelper {

    private val cartoonDrawables = listOf(
        R.drawable.ic_avatar_cartoon_1,
        R.drawable.ic_avatar_cartoon_2,
        R.drawable.ic_avatar_cartoon_3,
        R.drawable.ic_avatar_cartoon_4,
        R.drawable.ic_avatar_cartoon_5,
        R.drawable.ic_avatar_cartoon_6
    )

    /**
     * Deterministically maps any seed (userId, name, or phone) to a local 2D cartoon vector drawable.
     * Guaranteed 100% offline availability with zero network lag.
     */
    fun getCartoonAvatarDrawableRes(seed: String): Int {
        if (seed.isBlank()) return R.drawable.ic_avatar_cartoon_1
        val index = abs(seed.hashCode()) % cartoonDrawables.size
        return cartoonDrawables[index]
    }

    /**
     * Generates a high-definition 2D cartoon person avatar PNG URL using DiceBear Personas with soft pastel backgrounds.
     */
    fun getCartoonAvatarUrl(seed: String): String {
        val safeSeed = seed.ifBlank { "loanzo_member" }
        val encodedSeed = try {
            URLEncoder.encode(safeSeed, "UTF-8")
        } catch (_: Exception) {
            safeSeed.filter { it.isLetterOrDigit() }
        }
        return "https://api.dicebear.com/7.x/personas/png?seed=$encodedSeed&backgroundColor=b6e3f4,c0aede,d1d4f9,ffd5dc,ffdfbf"
    }
}

/**
 * Returns the effective avatar URL for a user.
 * If the user has uploaded a photo, returns that photo URI/URL.
 * Otherwise, returns a deterministic 2D cartoon avatar URL.
 */
fun UserEntity?.getEffectiveAvatarUrl(): String {
    if (this == null) return CartoonAvatarHelper.getCartoonAvatarUrl("guest")
    if (profilePhotoUri.isNotBlank()) return profilePhotoUri
    val seed = name.ifBlank { userId }
    return CartoonAvatarHelper.getCartoonAvatarUrl(seed)
}

/**
 * Returns the local 2D cartoon avatar vector drawable resource ID for a user.
 */
fun UserEntity?.getCartoonAvatarRes(): Int {
    if (this == null) return R.drawable.ic_avatar_cartoon_1
    val seed = name.ifBlank { userId }
    return CartoonAvatarHelper.getCartoonAvatarDrawableRes(seed)
}

/**
 * Returns the effective avatar URL for a marketplace post.
 * If the post has an authorAvatarUrl, returns it; otherwise generates a deterministic 2D cartoon avatar URL.
 */
fun MarketplacePostEntity.getEffectiveAvatarUrl(): String {
    if (authorAvatarUrl.isNotBlank()) return authorAvatarUrl
    val seed = authorName.ifBlank { authorId }
    return CartoonAvatarHelper.getCartoonAvatarUrl(seed)
}

/**
 * Returns the local 2D cartoon avatar vector drawable resource ID for a marketplace post.
 */
fun MarketplacePostEntity.getCartoonAvatarRes(): Int {
    val seed = authorName.ifBlank { authorId }
    return CartoonAvatarHelper.getCartoonAvatarDrawableRes(seed)
}
