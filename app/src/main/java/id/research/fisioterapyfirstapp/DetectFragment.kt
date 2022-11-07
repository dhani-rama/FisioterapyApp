package id.research.fisioterapyfirstapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import id.research.fisioterapyfirstapp.databinding.FragmentDetectBinding


class DetectFragment : Fragment() {

    private lateinit var mDetectBinding: FragmentDetectBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        mDetectBinding = FragmentDetectBinding.inflate(inflater, container, false)
        return mDetectBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mDetectBinding.btnDetect.setOnClickListener() {
            startActivity(Intent(this.requireActivity(), FirstDetectPoseActivity::class.java))
            //activity?.finish()
        }
    }
}