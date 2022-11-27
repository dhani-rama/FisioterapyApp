package id.research.fisioterapyfirstapp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import id.research.fisioterapyfirstapp.databinding.FragmentHomeBinding


class HomeFragment : Fragment() {

    private lateinit var mHomeBinding: FragmentHomeBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mHomeBinding = FragmentHomeBinding.inflate(inflater, container, false)
        // Inflate the layout for this fragment
        return mHomeBinding.root
    }

}