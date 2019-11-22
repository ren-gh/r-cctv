package cn.rengh.cctv.utils

import cn.rengh.cctv.R
import java.util.*

object ColorsUtils {
    private val mColorIds = intArrayOf(
        R.color.amber, R.color.brown, R.color.cyan,
        R.color.deepPurple, R.color.green, R.color.lightBlue,
        R.color.lightGreen, R.color.lime, R.color.orange,
        R.color.pink, R.color.cyan, R.color.deepPurple
    )

    @JvmStatic
    val randColor: Int
        get() {
            val random = Random()
            val pos = random.nextInt(mColorIds.size)
            return mColorIds[pos]
        }
}