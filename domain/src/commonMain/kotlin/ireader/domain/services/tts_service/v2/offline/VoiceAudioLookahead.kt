package ireader.domain.services.tts_service.v2.offline

import kotlinx.coroutines.CoroutineScope

/** One prepared sentence, retained across natural paragraph boundaries only. */
class VoiceAudioLookahead<A>(scope:CoroutineScope,generate:suspend (String,Float)->A) {
    private val buffer=VoiceAudioBuffer(scope,generate)
    private val lock=Any()
    private var followingParagraph:String?=null
    private var followingSentence:String?=null
    private var canPrepare=false
    private var rate=1f

    fun begin(keepPrepared:Boolean)=synchronized(lock) {
        followingParagraph=null;followingSentence=null;canPrepare=false
        if(!keepPrepared)buffer.clear()
    }
    suspend fun takeOrGenerate(text:String,rate:Float):A=buffer.takeOrGenerate(text,rate)

    /** Called after consuming the current audio, before playing it. */
    fun currentAudioReady(nextSentence:String?,rate:Float)=synchronized(lock) {
        followingSentence=nextSentence;this.rate=rate;canPrepare=true
        prepare()
    }
    /** May arrive before or after the worker has prepared its current sentence. */
    fun announceNextParagraph(firstSentence:String?)=synchronized(lock) {
        followingParagraph=firstSentence
        if(canPrepare && followingSentence==null)prepare()
    }
    private fun prepare() {
        (followingSentence ?: followingParagraph)?.let {buffer.prefetch(it,rate)}
    }
}
