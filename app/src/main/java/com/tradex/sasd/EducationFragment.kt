package com.tradex.sasd

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.tradex.sasd.databinding.FragmentEducationBinding

class EducationFragment : Fragment() {

    private var _binding: FragmentEducationBinding? = null
    private val binding get() = _binding!!

    private val adapter = EducationAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEducationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerEducation.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerEducation.adapter = adapter

        val articles = listOf(
            EducationArticle(
                title = "Co to jest kryptowaluta i blockchain?",
                meta = "5 min • Podstawy",
                preview = "Kryptowaluta to cyfrowy pieniądz działający w sieci bez centralnego pośrednika. Blockchain to rozproszona „księga” transakcji.",
                body = """
Kryptowaluta to cyfrowy token, który może służyć jako środek wymiany lub aktywo inwestycyjne.

Blockchain (łańcuch bloków) to rozproszona baza danych:
• transakcje są grupowane w bloki,
• bloki są łączone kryptograficznie,
• kopie bazy ma wiele węzłów w sieci.

W tej aplikacji obrót jest symulowany — bez prawdziwego blockchaina.
                """.trimIndent()
            ),
            EducationArticle(
                title = "Kurs, market cap i wolumen — jak to czytać?",
                meta = "4 min • Rynek",
                preview = "Cena to kurs jednostki. Market cap to cena × podaż. Wolumen pomaga ocenić aktywność handlu (płynności).",
                body = """
1) Cena (price) — bieżący kurs 1 jednostki.
2) Market cap — cena × liczba monet w obiegu.
3) Wolumen — ile aktywa było handlowane w danym okresie (np. 24h).

Tip: „tani coin” nie znaczy „okazja” bez kontekstu market cap i podaży.
                """.trimIndent()
            ),
            EducationArticle(
                title = "Ryzyko: dlaczego krypto potrafi mocno spadać?",
                meta = "6 min • Ryzyko",
                preview = "Zmienność wynika z płynności, nastrojów i dźwigni. Ustal zasady ryzyka zanim kupisz.",
                body = """
Krypto jest zmienne m.in. dlatego, że rynek reaguje szybko, działa 24/7,
a część projektów ma niską płynność.

Dobre nawyki:
• nie ryzykuj środków, których nie możesz stracić,
• dywersyfikuj,
• nie gonić FOMO,
• miej plan (kiedy realizujesz zysk/stratę).
                """.trimIndent()
            )
        )

        adapter.submitList(articles)

        adapter.onItemClick = { article ->
            EducationArticleDialogFragment
                .newInstance(article.title, article.body)
                .show(childFragmentManager, "edu_article")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}