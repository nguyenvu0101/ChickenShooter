package com.example.chickenshooter

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class CartActivity : AppCompatActivity() {

    private var coins: Long = 0L

    // Giá động (set ở đây, chỉ cần sửa code là giao diện tự đổi)
    private val pricePlane1 = 5L
    private val pricePlane2 = 1200L
    private val priceBullet1 = 100L
    private val priceBullet2 = 350L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        val prefs = getSharedPreferences("game", MODE_PRIVATE)
        coins = prefs.getLong("coins", 0L)

        val tvCoins = findViewById<TextView>(R.id.tvCoinsInCart)
        tvCoins.text = "Xu: $coins"

        // Set giá động cho từng loại
        val tvPricePlane1 = findViewById<TextView>(R.id.tvPricePlane1)
        val tvPricePlane2 = findViewById<TextView>(R.id.tvPricePlane2)
        val tvPriceBullet1 = findViewById<TextView>(R.id.tvPriceBullet1)
        val tvPriceBullet2 = findViewById<TextView>(R.id.tvPriceBullet2)
        tvPricePlane1.text = "Giá: $pricePlane1 xu"
        tvPricePlane2.text = "Giá: $pricePlane2 xu"
        tvPriceBullet1.text = "Giá: $priceBullet1 xu"
        tvPriceBullet2.text = "Giá: $priceBullet2 xu"

        val ownedPlanes = prefs.getStringSet("owned_planes", emptySet()) ?: emptySet()
        val currentPlane = prefs.getString("current_plane", null)

        val btnBuyPlane1 = findViewById<Button>(R.id.btnBuyPlane1)
        val btnBuyPlane2 = findViewById<Button>(R.id.btnBuyPlane2)

        setupPlaneButton(btnBuyPlane1, "player_cart1", pricePlane1, "Máy bay thường", ownedPlanes, currentPlane, tvCoins)
        setupPlaneButton(btnBuyPlane2, "player_cart2", pricePlane2, "Máy bay siêu tốc", ownedPlanes, currentPlane, tvCoins)

        val ownedBullets = prefs.getStringSet("owned_bullets", emptySet()) ?: emptySet()
        val btnBuyBullet1 = findViewById<Button>(R.id.btnBuyBullet1)
        val btnBuyBullet2 = findViewById<Button>(R.id.btnBuyBullet2)

        setupBulletButton(btnBuyBullet1, "bullet_cart1", priceBullet1, "Đạn thường", ownedBullets, tvCoins)
        setupBulletButton(btnBuyBullet2, "bullet_cart2", priceBullet2, "Đạn xuyên phá", ownedBullets, tvCoins)
    }

    private fun setupPlaneButton(
        btn: Button,
        planeId: String,
        price: Long,
        itemName: String,
        ownedPlanes: Set<String>,
        currentPlane: String?,
        tvCoins: TextView
    ) {
        if (!ownedPlanes.contains(planeId)) {
            btn.text = "Mua"
            btn.isEnabled = true
            btn.setOnClickListener {
                if (coins >= price) {
                    coins -= price
                    val prefs = getSharedPreferences("game", MODE_PRIVATE)
                    prefs.edit().putLong("coins", coins).apply()
                    tvCoins.text = "Xu: $coins"
                    addPlaneToCollection(planeId)
                    btn.text = "Dùng"
                    Toast.makeText(this, "Mua $itemName thành công!", Toast.LENGTH_SHORT).show()
                    btn.setOnClickListener {
                        setCurrentUsedPlane(planeId)
                        goToStartMenu()
                    }
                } else {
                    Toast.makeText(this, "Không đủ xu!", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            btn.text = "Dùng"
            btn.isEnabled = true
            btn.setOnClickListener {
                setCurrentUsedPlane(planeId)
                goToStartMenu()
            }
        }
    }

    private fun setupBulletButton(
        btn: Button,
        bulletId: String,
        price: Long,
        itemName: String,
        ownedBullets: Set<String>,
        tvCoins: TextView
    ) {
        if (!ownedBullets.contains(bulletId)) {
            btn.text = "Mua"
            btn.isEnabled = true
            btn.setOnClickListener {
                if (coins >= price) {
                    coins -= price
                    val prefs = getSharedPreferences("game", MODE_PRIVATE)
                    prefs.edit().putLong("coins", coins).apply()
                    tvCoins.text = "Xu: $coins"
                    addBulletToCollection(bulletId)
                    btn.text = "Dùng"
                    btn.isEnabled = false
                    Toast.makeText(this, "Mua $itemName thành công!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Không đủ xu!", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            btn.text = "Dùng"
            btn.isEnabled = false
        }
    }

    private fun addPlaneToCollection(planeId: String) {
        val prefs = getSharedPreferences("game", MODE_PRIVATE)
        val planesSet = prefs.getStringSet("owned_planes", mutableSetOf()) ?: mutableSetOf()
        planesSet.add(planeId)
        prefs.edit().putStringSet("owned_planes", planesSet).apply()
    }

    private fun addBulletToCollection(bulletId: String) {
        val prefs = getSharedPreferences("game", MODE_PRIVATE)
        val bulletsSet = prefs.getStringSet("owned_bullets", mutableSetOf()) ?: mutableSetOf()
        bulletsSet.add(bulletId)
        prefs.edit().putStringSet("owned_bullets", bulletsSet).apply()
    }

    private fun setCurrentUsedPlane(planeId: String) {
        val prefs = getSharedPreferences("game", MODE_PRIVATE)
        prefs.edit().putString("current_plane", planeId).apply()
    }
    private fun setCurrentUsedBullet(bulletId: String) {
        val prefs = getSharedPreferences("game", MODE_PRIVATE)
        prefs.edit().putString("current_bullet", bulletId).apply()
    }
    private fun goToStartMenu() {
        val intent = Intent(this, StartMenuActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
        finish()
    }
}