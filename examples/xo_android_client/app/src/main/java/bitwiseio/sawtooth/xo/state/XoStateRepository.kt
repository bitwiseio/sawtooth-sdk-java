package bitwiseio.sawtooth.xo.state

import bitwiseio.sawtooth.xo.state.api.SawtoothRestApi
import retrofit2.Retrofit
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.util.Log
import com.google.common.io.BaseEncoding
import retrofit2.converter.gson.GsonConverterFactory
import bitwiseio.sawtooth.xo.models.Game
import bitwiseio.sawtooth.xo.state.api.StateResponse
import android.arch.lifecycle.MutableLiveData
import bitwiseio.sawtooth.xo.state.api.Entry

class XoStateRepository(url: String) {
    private var service: SawtoothRestApi? = null
    var games: MutableLiveData<List<Game>> = MutableLiveData()
    var gameFocus: MutableLiveData<Game> = MutableLiveData()
    private var restApiURL: String = url

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl(restApiURL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        service = retrofit.create<SawtoothRestApi>(SawtoothRestApi::class.java)
    }

    fun getState(update: Boolean, url: String) {
        checkURLChanged(url)
        val resp = arrayListOf<Game>()
        if (update) {
            service?.getState(transactionFamilyPrefix())?.enqueue(object : Callback<StateResponse> {
                override fun onResponse(call: Call<StateResponse>, response: Response<StateResponse>) {
                    if (response.body() != null) {
                        response.body()?.data?.map { entry ->

                            resp.add(parseGame(entry.data))
                        }
                        games.value = resp.sortedBy { it.name.toLowerCase() }

                        Log.d("XO.State", "Updated game list")
                    } else {
                        Log.d("XO.State", response.toString())
                    }
                }
                override fun onFailure(call: Call<StateResponse>, t: Throwable) {
                    Log.d("XO.State", t.toString())
                    call.cancel()
                }
            })
        }
    }

    fun getGameState(name: String, url: String) {
        checkURLChanged(url)
        val gameAddress = makeGameAddress(name)
        service?.getState(gameAddress)?.enqueue(object : Callback<StateResponse> {
            override fun onResponse(call: Call<StateResponse>, response: Response<StateResponse>) {
                if (response.body() != null) {
                    val entry: Entry? = response.body()?.data?.get(0)
                    val gameData: Game = entry?.data?.let { parseGame(it) }!!
                    gameFocus.value = gameData
                    Log.d("XO.State", "Updated game state")
                } else {
                    Log.d("XO.State", response.toString())
                }
            }
            override fun onFailure(call: Call<StateResponse>, t: Throwable) {
                Log.d("XO.State", t.toString())
                call.cancel()
            }
        })
    }

    private fun parseGame(data: String): Game {
        val decoded = String(BaseEncoding.base64().decode(data))
        val split = decoded.split(',')
        return Game(split[0], split[1], split[2], split[3], split[4])
    }

    private fun checkURLChanged(url: String) {
        if (restApiURL != url) {
            restApiURL = url
            buildService()
        }
    }

    private fun buildService() {
        val retrofit = Retrofit.Builder()
            .baseUrl(restApiURL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        service = retrofit.create<SawtoothRestApi>(SawtoothRestApi::class.java)
    }
}
