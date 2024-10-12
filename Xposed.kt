

class Xposed : IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {

        kotlin.runCatching {
            XposedHelpers.findAndHookMethod("com.tencent.smtt.sdk.WebView", lpparam.classLoader, "loadUrl",
                String::class.java, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        super.beforeHookedMethod(param)
                        val url = param.args[0] as String
                        logD("loadUrl : $url")
//                        printStackInfo()
                    }

//                    @Throws(Throwable::class)
//                    override fun afterHookedMethod(param: MethodHookParam) {
//                        super.afterHookedMethod(param)
//                    }
                })

//            XposedHelpers.findAndHookConstructor("com.yuanfudao.android.common.webview.base.JsBridgeBean\$a",
//                lpparam.classLoader,
//                XposedHelpers.findClass("com.yuanfudao.android.common.webview.base.a",lpparam.classLoader),
//                String::class.java,
//                String::class.java,
//                object : XC_MethodHook() {
//                    override fun beforeHookedMethod(param: MethodHookParam) {
//                        super.beforeHookedMethod(param)
//                        for(index in 0 until param.args.size){
//                            logD("$index : ${param.args[index]}")
//                        }
//                        logD("原始信息：${param.args[2].toString()}")
//                        val decodeJsonArray = JSONArray.parse(decodeBase64(param.args[2].toString()))
//                        logD("原始信息：${decodeJsonArray}")
//                        val decodeInfo = if (decodeJsonArray.count() > 1){
//                            decodeJsonArray[1]
//                        }else{
//                            decodeJsonArray[0]
//                        }
//                        logD("解密信息：${decodeInfo}")
//
//                        val typeArg = param.args[1].toString().split("_")[0]
//
//                        when(typeArg){
//                            "dataDecrypt"->{
//                                val rankInfo = JSONObject.parseObject(decodeJsonArray[1].toString()).getString("result")
//                                logD("show info : ${decodeDataDecrypt(rankInfo)}")
//                            }
//                            "recognize"->{
//
//                            }
//                        }
//                    }
//
//                })
            val answers = mutableListOf<String>()
            XposedHelpers.findAndHookMethod(
                "com.yuanfudao.android.common.webview.base.JsBridgeBean\$a",
                lpparam.classLoader,
                "run",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val thisObject = param.thisObject
                        val webView = XposedHelpers.getObjectField(thisObject,"a")
                        val type = XposedHelpers.getObjectField(thisObject,"b")
                        val typeArg = type.toString().split("_")[0]
                        val content = XposedHelpers.getObjectField(thisObject,"c")
                        val decodeJsonArray = JSONArray.parse(decodeBase64(content.toString()))
                        logD("原始信息：${decodeJsonArray}")
                        val decodeInfo = if (decodeJsonArray.count() > 1){
                            decodeJsonArray[1]
                        }else{
                            decodeJsonArray[0]
                        }
                        logD("解密信息：${decodeInfo}")

                        when(typeArg){
                            "dataDecrypt"->{
                                val rankInfo = JSONObject.parseObject(decodeJsonArray[1].toString()).getString("result")
                                logD("show info : ${decodeDataDecrypt(rankInfo)}")
                                answers.clear()
                                answers.addAll(getAnswers(decodeDataDecrypt(rankInfo)))
                            }
                            "recognize"->{
                                for (answer in answers){
                                    val sendContent = encodeBase64("[null,\"${answer}\"]")
                                    logD("send answer : $answer -> $sendContent")
                                    XposedHelpers.callMethod(webView,"loadUrl", "javascript:(window.$type && window.$type(\"${sendContent}\"))")
                                }
                            }
                        }
                    }

                })
        }
    }

    fun decodeBase64(base64: String): String {
        val base64EncodedString = base64 // 这实际上是 "Hello World!" 的Base64编码

        // 创建一个Base64解码器
        val decoder = Base64.getDecoder()

        // 解码字符串
        val decodedBytes = decoder.decode(base64EncodedString)

        // 将字节数组转换为字符串
        return String(decodedBytes)
    }

    fun encodeBase64(content: String): String{
        val encoder = Base64.getEncoder()
        return encoder.encodeToString(content.toByteArray())
    }

    fun decodeDataDecrypt(content: String):String{
        val fixBase64 = content.replace("\n", "")
        logD("check origin content : $fixBase64")
        return decodeBase64(fixBase64)
    }

    fun getAnswers(json:String):List<String>{
        val answerList = mutableListOf<String>()
        kotlin.runCatching {
            val jsonObject = JSONObject.parseObject(json)
            val examVO =jsonObject.getJSONObject("examVO")
            val questions = examVO.getJSONArray("questions")
            for (i in 0 until questions.size){
                val question = questions[i] as JSONObject
                val answer = question.getString("answer")
                answerList.add(answer)
            }
        }
        return answerList
    }

}