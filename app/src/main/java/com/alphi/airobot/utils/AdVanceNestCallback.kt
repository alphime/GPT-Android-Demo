package com.alphi.airobot.utils

class AdVanceNestCallback(val callback: () -> Unit) {

    init {
        load1()
    }

    private fun load1() {
        val cc1 = callback
        One.run(cc1)
    }
}


class One private constructor(k: () -> Unit){
    companion object {
        fun run(k: () -> Unit) {
            val k1 = k
            One(k).mtd1(k1)
        }
    }

    private fun mtd1(k: () -> Unit) {
        val k5 = k
        One(k).mtd2(k5)
    }
    private fun mtd2(k: () -> Unit) {
        val k8 = k
        mtd3(k8)
    }
    private fun mtd3(k: () -> Unit) {
        val kk = k
        Tow(kk).run()
    }
}

class Tow(val callback: () -> Unit) {

    fun run() {
        val mm = callback
        mtd1(mm)
    }

    private fun mtd1(k: () -> Unit) {
        val mm = callback
        Tow(k).mtd2(mm)
    }
    private fun mtd2(k: () -> Unit) {
        k()
    }
}