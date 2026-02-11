
package com.tradex.sasd

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var btnMarket: TextView
    private lateinit var btnPortfolio: TextView
    private lateinit var btnEducation: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnMarket = findViewById(R.id.btnMarket)
        btnPortfolio = findViewById(R.id.btnPortfolio)
        btnEducation = findViewById(R.id.btnEducation)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, CoinListFragment())
                .commit()
            setActiveButton(btnMarket)
        }

        btnMarket.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, CoinListFragment())
                .commit()
            setActiveButton(btnMarket)
        }

        btnPortfolio.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, PortfolioFragment())
                .commit()
            setActiveButton(btnPortfolio)
        }

        btnEducation.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, EducationFragment())
                .commit()
            setActiveButton(btnEducation)
        }
    }

    private fun setActiveButton(active: TextView) {
        val inactive = ContextCompat.getColor(this, android.R.color.darker_gray)
        val activeColor = ContextCompat.getColor(this, android.R.color.white)

        btnMarket.setTextColor(inactive)
        btnPortfolio.setTextColor(inactive)
        btnEducation.setTextColor(inactive)

        active.setTextColor(activeColor)
    }
}