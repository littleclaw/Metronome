package com.lttclaw.metronome.ui

import android.content.ContentUris
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.core.os.bundleOf
import com.blankj.utilcode.util.LogUtils
import com.drake.brv.utils.bindingAdapter
import com.drake.brv.utils.setup
import com.lttclaw.metronome.R
import com.lttclaw.metronome.databinding.ActivitySelectMusicBinding
import com.lttclaw.metronome.model.MusicItem
import com.lttclaw.metronome.viewmodel.BgMusicSelectVm
import me.hgj.jetpackmvvm.base.activity.BaseVmDbActivity

class SelectMusicActivity : BaseVmDbActivity<BgMusicSelectVm, ActivitySelectMusicBinding>() {
    override fun createObserver() {
    }

    override fun dismissLoading() {
    }

    override fun initView(savedInstanceState: Bundle?) {
        mDatabind.rvMusic.setup {
            addType<MusicItem>(R.layout.item_music)
            onChecked { position, checked, allChecked ->
                val m = getModel<MusicItem>(position)
                m.selected = checked
                m.notifyChange()
            }
            singleMode = true
            onClick(R.id.item_music_wrapper){
                val m = getModel<MusicItem>()
                val isChecked = m.selected
                mViewModel.curSelectedItem = m
                mViewModel.curSelectedName.value = m.name
                setChecked(bindingAdapterPosition, isChecked.not())
            }
        }.models = initMusicList()
        mDatabind.btnOk.setOnClickListener {
            val intent = intent
            intent.data = mViewModel.curSelectedItem?.uri
            intent.putExtra("name", mViewModel.curSelectedItem?.name)
            intent.putExtra("duration", mViewModel.curSelectedItem?.duration)
            setResult(RESULT_OK, intent)
            finish()
        }
        mDatabind.btnCancel.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
        mDatabind.vm = mViewModel
    }

    override fun showLoading(message: String) {
    }

    private fun initMusicList(): MutableList<MusicItem> {
        val musicList = mutableListOf<MusicItem>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE
        )
        val sortOrder = "${MediaStore.Audio.Media.DISPLAY_NAME} ASC"
        val query = contentResolver.query(collection, projection, null, null, sortOrder)
        query?.use { cursor->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            while (cursor.moveToNext()){
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val duration = cursor.getInt(durationColumn)
                val size = cursor.getInt(sizeColumn)
                val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                musicList += MusicItem(contentUri, name, duration, size)
            }
        }
        LogUtils.d(musicList.size)
        return musicList
    }
}