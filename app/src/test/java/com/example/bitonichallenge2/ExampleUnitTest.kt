package com.example.bitonichallenge2

import androidx.lifecycle.MutableLiveData
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {

        val list1 = mutableListOf<Int>(1,2,3)
        var list2 = mutableListOf<Int>()

        for (i in list1){
            list2.add(i)
        }

        list2.removeAt(0)

        println("List1 ${list1} List2 $list2")

    }
}