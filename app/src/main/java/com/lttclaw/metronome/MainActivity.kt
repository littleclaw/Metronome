package com.lttclaw.metronome

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.Engine.KEY_PARAM_VOLUME
import android.view.View
import android.view.View.OnClickListener
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import java.lang.Integer.parseInt
import java.lang.Long.parseLong
import java.util.Locale

class MainActivity : AppCompatActivity(),OnClickListener, TextToSpeech.OnInitListener {
    var etNum: EditText? = null
    var etLength: EditText? = null
    var tvCur: TextView? = null
    var btnStart: Button? = null
    var btnStop: Button? = null
    var mExitTime: Long = 0L
    var timer: CountDownTimer? = null
    var text2Speech: TextToSpeech? = null
    var currentNum: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        etNum = findViewById(R.id.et_number)
        etLength = findViewById(R.id.et_length)
        btnStart = findViewById(R.id.btn_start)
        btnStop = findViewById(R.id.btn_stop)
        tvCur = findViewById(R.id.tv_current)
        btnStart!!.setOnClickListener(this)
        btnStop!!.setOnClickListener(this)

        text2Speech = TextToSpeech(baseContext, this)
        onBackPressedDispatcher.addCallback {
            if(System.currentTimeMillis() - mExitTime > 2000){
                finish()
            }else{
                mExitTime = System.currentTimeMillis()
                Toast.makeText(baseContext, "再按一次退出", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onClick(v: View?) {
        hideKeyboard()
        when(v?.id){
            R.id.btn_start -> {
                initTimer()
            }
            R.id.btn_stop -> {
                stopTimer()
            }
        }
    }

    private fun initTimer(){
        val numInput = etNum?.text
        val lengthInput = etLength?.text
        if(numInput.isNullOrEmpty() || lengthInput.isNullOrEmpty()){
            Toast.makeText(baseContext, "请检查输入是否正确", Toast.LENGTH_SHORT).show()
        }else{
            try {
                val num = parseInt(numInput.toString())
                val length = parseLong(lengthInput.toString())
                currentNum = 1
                timer = object: CountDownTimer(num*length, length){
                    override fun onTick(millisUntilFinished: Long) {
                        val str = currentNum.toString()
                        tvCur?.text = str
                        speak(str)
                        currentNum++
                    }

                    override fun onFinish() {
                        currentNum = 0
                        speak("好棒棒，你完成啦")
                        tvCur?.text = ""
                        btnStop!!.visibility = View.INVISIBLE
                        btnStart!!.visibility = View.VISIBLE
                    }
                }
                timer?.also {
                    it.start()
                    btnStart!!.visibility = View.INVISIBLE
                    btnStop!!.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                btnStart!!.visibility = View.VISIBLE
                Toast.makeText(baseContext, "参数转数字错误", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun stopTimer(){
        timer?.also {
            it.cancel()
        }
        btnStop!!.visibility = View.INVISIBLE
        btnStart!!.visibility = View.VISIBLE
        tvCur?.text = ""
    }

    private fun speak(s:String){
        text2Speech?.also {
            val bundle = bundleOf(KEY_PARAM_VOLUME to 1f)
            it.speak(s, TextToSpeech.QUEUE_ADD, bundle, currentNum.toString())
        }
    }

    override fun onInit(status: Int) {
        if(status == TextToSpeech.SUCCESS){
            val languageCode = text2Speech?.setLanguage(Locale.CHINESE)
            if(languageCode==null || languageCode == TextToSpeech.LANG_NOT_SUPPORTED){
                Toast.makeText(baseContext, "不支持中文", Toast.LENGTH_SHORT).show()
            }else {
                text2Speech!!.language = Locale.CHINA
            }
            text2Speech?.let {
                it.setPitch(1f)
                it.setSpeechRate(1f)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        text2Speech?.also {
            it.stop()
            it.shutdown()
        }
    }

    private fun Activity.hideKeyboard() {
        hideKeyboard(currentFocus ?: View(this))
    }

    private fun Context.hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

}