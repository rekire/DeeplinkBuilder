package eu.rekisoft.android.navbug

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import eu.rekisoft.android.navbug.databinding.FragmentShelfBinding

class ShelfFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_shelf, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        with(FragmentShelfBinding.bind(view)) {
            listOf(book1, book2).forEach { book ->
                book.setOnClickListener {
                    findNavController().navigate(ShelfFragmentDirections.openBook(book.text.toString()))
                }
            }
        }
    }
}