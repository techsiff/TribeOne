package com.siffmember.info.ui.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.siffmember.info.ui.model.MessageAI
import com.siffmember.info.ui.repository.GeminiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


class ChatViewModel(private val repository: GeminiRepository) : ViewModel() {

    private val _messages = MutableLiveData<MutableList<MessageAI>>(mutableListOf())
    val messages: LiveData<MutableList<MessageAI>> get() = _messages

    private val _errorMessage: MutableStateFlow<String?> = MutableStateFlow(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _generatedText = MutableStateFlow<String?>(null)
    val generatedText: StateFlow<String?> = _generatedText

    /*fun sendMessageGeneral(prompt: String, base64File: String, mime: String, apiKey: String) {
        _messages.value?.add(MessageAI(prompt, isUser = true, isTyping = false))
        _messages.postValue(_messages.value)
        // Add typing indicator
        _messages.value?.add(MessageAI("TribeOne is thinking...", isUser = false, isTyping = true))
        _messages.postValue(_messages.value)
        repository.generateContent(prompt, base64File, mime, apiKey) { response ->
            // Remove typing indicator
            _messages.value?.removeIf { it.isTyping }
            // Add actual response
            _messages.value?.add(MessageAI(response ?: "No response",
                isUser = false,
                isTyping = false
            ))
            _messages.postValue(_messages.value)
        }
    }*/

    fun sendMessageGeneral(prompt: String, files: List<Pair<String, String>>, mime: String, apiKey: String) {
        _messages.value?.add(MessageAI(prompt, isUser = true, isTyping = false))
        _messages.postValue(_messages.value)
        // Add typing indicator
        _messages.value?.add(MessageAI("TribeOne is thinking...", isUser = false, isTyping = true))
        _messages.postValue(_messages.value)
        repository.generateContentMultiple(prompt, files, apiKey) { response ->
            // Remove typing indicator
            _messages.value?.removeIf { it.isTyping }
            // Add actual response
            _messages.value?.add(MessageAI(response ?: "No response",
                isUser = false,
                isTyping = false
            ))
            _messages.postValue(_messages.value)
        }
    }

    fun sendMessage(prompt: String, base64File: String, mime: String, apiKey: String) {
       // _messages.value?.add(MessageAI(prompt, isUser = true, isTyping = false))
       // _messages.postValue(_messages.value)
        // Add typing indicator
        _messages.value?.add(MessageAI("TribeOne is thinking...", isUser = false, isTyping = true))
        _messages.postValue(_messages.value)
        repository.generateContent(prompt, base64File, mime, apiKey) { response ->
            // Remove typing indicator
            _messages.value?.removeIf { it.isTyping }
            // Add actual response
            _messages.value?.add(MessageAI(response ?: "No response",
                isUser = false,
                isTyping = false
            ))
            _messages.postValue(_messages.value)
        }
    }

    fun addBotMessage(message: String) {
        val list = _messages.value ?: mutableListOf()
        list.add(MessageAI(message, isUser = false, isTyping = false))
        _messages.value = list
    }

    fun addUserMessage(message: String) {
        val list = _messages.value ?: mutableListOf()
        list.add(MessageAI(message, isUser = true, isTyping = false))
        _messages.value = list
    }

    fun validateAnswer(
        question: String,
        answer: String,
        apiKey: String,
        onResult: (Boolean) -> Unit
    ) {
        Log.e("validateAnswer Request","question:: $question  answer:: $answer")
        viewModelScope.launch {
            val isValid = repository.validateAnswer(question, answer, apiKey)
            Log.e("validateAnswer response","validateAnswer response : $isValid")
            onResult(isValid)
        }
    }
}