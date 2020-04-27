package com.`in`.videoassignment.ui.video

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager2.widget.ViewPager2
import com.`in`.videoassignment.R
import com.`in`.videoassignment.data.Response
import com.`in`.videoassignment.data.Video


private const val ARG_PARAM = "response"

class VideoFragment : Fragment() {
    private var response: Response? = null
    private lateinit var mpager:ViewPager2
    private var currentVideoPosition:Int=0
    private var pagerAdapter:RecyAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            response = it.get(ARG_PARAM) as Response
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        var view  = inflater.inflate(R.layout.fragment_video, container, false)
        mpager = view.findViewById(R.id.vertical_view_pager)
        pagerAdapter = activity?.let { RecyAdapter(it,response?.Response as List<Video>) }
        mpager.adapter = pagerAdapter
        mpager.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                currentVideoPosition = position
            }
        })
        return view
    }

    override fun onPause() {
        super.onPause()
        pagerAdapter?.onPause(currentVideoPosition)
    }

    override fun onResume() {
        super.onResume()
        pagerAdapter?.onResume(currentVideoPosition)
    }

    companion object {
        @JvmStatic
        fun newInstance(response: Response) =
            VideoFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_PARAM, response)
                }
            }
    }
}
