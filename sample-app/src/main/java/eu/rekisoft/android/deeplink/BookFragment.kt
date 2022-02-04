package eu.rekisoft.android.deeplink

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import eu.rekisoft.android.deeplink.databinding.FragmentBookBinding

class BookFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_book, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        with(FragmentBookBinding.bind(view)) {
            content.text = "Here is the content of " + arguments?.getString("name", "I hate bugs")
        }
    }
}