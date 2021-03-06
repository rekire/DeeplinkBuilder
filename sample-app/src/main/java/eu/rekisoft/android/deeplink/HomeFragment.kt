package eu.rekisoft.android.deeplink

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import eu.rekisoft.android.deeplink.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        with(FragmentHomeBinding.bind(view)) {
            button1.setOnClickListener {
                findNavController().navigate(HomeFragmentDirections.causeBug())
            }
            button2.setOnClickListener {
                findNavController().navigate(BookFragmentDeeplink.create("example"))
            }
        }
    }
}