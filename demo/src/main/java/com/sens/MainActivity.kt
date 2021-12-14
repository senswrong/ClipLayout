package com.sens

import android.app.Activity
import android.graphics.drawable.Animatable
import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.TextView
import android.widget.Toast

/**
 * Created by Sens on 2021/12/12.
 * {@see <a href="https://github.com/senswrong/ClipLayout">ClipLayout</a>}
 */
class MainActivity : Activity() {
    private fun toast(message: String) {
        hint.text = message
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private val hint by lazy {
        findViewById(R.id.hint) as TextView
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        (findViewById(R.id.background).background as AnimationDrawable).start()
        (findViewById(R.id.avm1).background as Animatable).start()
        findViewById(R.id.background).apply {
            setOnClickListener(object : View.OnClickListener {
                override fun onClick(v: View?) {
                    toast("bg")
                }
            })
        }
        findViewById(R.id.avm1).apply {
            setOnClickListener(object : View.OnClickListener {
                override fun onClick(v: View?) {
                    toast("mask")
                }
            })
        }
        findViewById(R.id.avatar).apply {
            setOnClickListener(object : View.OnClickListener {
                override fun onClick(v: View?) {
                    toast("avatar")
                }
            })
        }
        findViewById(R.id.name).apply {
            setOnClickListener(object : View.OnClickListener {
                override fun onClick(v: View?) {
                    toast("SENS")
                }
            })
        }

        findViewById(R.id.yin).apply {
            setOnClickListener(object : View.OnClickListener {
                override fun onClick(v: View?) {
                    toast("Sens")
                }
            })
        }

        findViewById(R.id.yang).apply {
            setOnClickListener(object : View.OnClickListener {
                override fun onClick(v: View?) {
                    toast("NeZha")
                }
            })
        }

        findViewById(R.id.yinyang).startAnimation(RotateAnimation(
            0f,
            360f,
            Animation.RELATIVE_TO_SELF,
            0.5f,
            Animation.RELATIVE_TO_SELF,
            0.5f
        ).apply {
            duration = 5000
            repeatCount = Animation.INFINITE
            fillAfter = true
            interpolator = LinearInterpolator()
        })
    }
}