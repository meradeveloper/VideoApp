package com.`in`.videoassignment.ui.video

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.`in`.videoassignment.R
import com.`in`.videoassignment.data.Video
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.LoopingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelection
import com.google.android.exoplayer2.upstream.BandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import kotlinx.android.synthetic.main.fragment_pager.view.*


class RecyAdapter : RecyclerView.Adapter<RecyAdapter.ViewHolder> {

    private var videos = listOf<Video>()
    private var mcontext:Context
    private var videostatus = mutableListOf<VideoStatus>()
    constructor(context: Context, videos:List<Video>)
    {
        this.videos =videos
        this.mcontext = context
    }

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        super.onViewAttachedToWindow(holder)
        if(videostatus.get(holder.adapterPosition)==VideoStatus.PAUSE)
            holder.stopVideo()
        else
            holder.bindVideo(videos.get(holder.adapterPosition),mcontext)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        var view = LayoutInflater.from(parent.context).inflate(R.layout.fragment_pager,parent,false)
        videostatus.add(VideoStatus.PLAY)
        return ViewHolder(view)
    }

    override fun getItemCount() = videos.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.releaseVideo()
    }

    fun onPause(position: Int)
    {
        if(videostatus.size>0)
        {
            videostatus.set(position,VideoStatus.PAUSE)
            notifyItemChanged(position)
        }
    }

    fun onResume(position: Int)
    {
        if(videostatus.size>0)
        {
            videostatus.set(position,VideoStatus.PLAY)
            notifyItemChanged(position)
        }
    }

    open class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)
    {
        var simpleExoPlayer:SimpleExoPlayer?=null
        fun bindVideo(video:Video,mcontext:Context)
        {
            if(!video.VideoUrl.isNullOrEmpty())
            {
                //val bandwidthMeter: BandwidthMeter = DefaultBandwidthMeter()
                //val factory: TrackSelection.Factory = AdaptiveTrackSelection.Factory(bandwidthMeter)
                val trackSelector = DefaultTrackSelector()
                trackSelector.setParameters(
                    trackSelector.buildUponParameters().setForceLowestBitrate(true))
                //simpleExoPlayer = ExoPlayerFactory.newSimpleInstance(mcontext,DefaultTrackSelector(AdaptiveTrackSelection.Factory(DefaultBandwidthMeter())),DefaultLoadControl())
                simpleExoPlayer = ExoPlayerFactory.newSimpleInstance(mcontext,trackSelector,DefaultLoadControl())

                var mediaDataSourceFactory = DefaultDataSourceFactory(mcontext, Util.getUserAgent(mcontext, "mediaPlayerSample"),DefaultBandwidthMeter())

                val mediaSource = ProgressiveMediaSource.Factory(mediaDataSourceFactory).createMediaSource(Uri.parse(video.VideoUrl))

                simpleExoPlayer?.prepare(LoopingMediaSource(mediaSource), false, false)
                simpleExoPlayer?.playWhenReady = true

                itemView.explayerview.setShutterBackgroundColor(mcontext.resources.getColor(android.R.color.transparent))
                itemView.explayerview.player = simpleExoPlayer
                itemView.explayerview.requestFocus()
            }

        }

        fun stopVideo()
        {
            simpleExoPlayer?.stop()
        }

        fun releaseVideo() {
            simpleExoPlayer?.release()
        }
    }

}