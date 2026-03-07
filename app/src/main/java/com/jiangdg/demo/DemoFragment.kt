/*
 * Copyright 2017-2022 Jiangdg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jiangdg.demo

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.jiangdg.ausbc.MultiCameraClient
import com.jiangdg.ausbc.base.CameraFragment
import com.jiangdg.ausbc.callback.ICameraStateCallBack
import com.jiangdg.ausbc.utils.bus.BusKey
import com.jiangdg.ausbc.utils.bus.EventBus
import com.jiangdg.ausbc.widget.AspectRatioTextureView
import com.jiangdg.ausbc.widget.IAspectRatio
import com.jiangdg.demo.databinding.FragmentDemoBinding

/** Simplified CameraFragment Usage Demo - UVC Camera View Only
 *
 * @author Created by jiangdg on 2022/1/28
 */
class DemoFragment : CameraFragment() {

    private lateinit var mViewBinding: FragmentDemoBinding

    override fun getRootView(inflater: LayoutInflater, container: ViewGroup?): View {
        mViewBinding = FragmentDemoBinding.inflate(inflater, container, false)
        return mViewBinding.root
    }

    override fun getCameraView(): IAspectRatio {
        return AspectRatioTextureView(requireContext())
    }

    override fun getCameraViewContainer(): ViewGroup {
        return mViewBinding.cameraViewContainer
    }

    override fun getGravity(): Int = Gravity.CENTER

    override fun initData() {
        super.initData()
        // Frame rate updates come via EventBus from CameraFragment
        EventBus.with<Int>(BusKey.KEY_FRAME_RATE).observe(this, {
            mViewBinding.frameRateTv.text = "frame rate:  $it fps"
        })
    }

    override fun onCameraState(
        self: MultiCameraClient.ICamera,
        code: ICameraStateCallBack.State,
        msg: String?
    ) {
        when (code) {
            ICameraStateCallBack.State.OPENED -> {
                mViewBinding.uvcLogoIv.visibility = View.GONE
                mViewBinding.frameRateTv.visibility = View.VISIBLE
            }
            ICameraStateCallBack.State.CLOSED -> {
                mViewBinding.uvcLogoIv.visibility = View.VISIBLE
                mViewBinding.frameRateTv.visibility = View.GONE
            }
            ICameraStateCallBack.State.ERROR -> {
                mViewBinding.uvcLogoIv.visibility = View.VISIBLE
                mViewBinding.frameRateTv.visibility = View.GONE
                Toast.makeText(requireContext(), "Camera error: $msg", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val TAG = "DemoFragment"
    }
}
