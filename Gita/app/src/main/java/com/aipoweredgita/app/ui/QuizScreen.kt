package com.aipoweredgita.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aipoweredgita.app.R
import com.aipoweredgita.app.ui.LoadingScreen
import com.aipoweredgita.app.viewmodel.QuizViewModel
import com.aipoweredgita.app.quiz.QuizContent
import com.aipoweredgita.app.ui.quiz.CompletionDialog
import com.aipoweredgita.app.ui.ErrorScreen

@Composable
fun QuizScreen(
    modifier: Modifier = Modifier,
    onExitQuiz: () -> Unit = {}, // Add callback to navigate back to home
    viewModel: com.aipoweredgita.app.viewmodel.QuizViewModel = viewModel()
) {
    val quizState by viewModel.quizState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (quizState.currentQuestion == null && !quizState.isLoading) {
            val online = com.aipoweredgita.app.utils.NetworkUtils.isNetworkAvailable(context)
            if (!online) {
                viewModel.setError("No Internet Connection\n\nPlease check your internet and try again.")
            } else {
                viewModel.loadNextQuestion()
            }
        }
    }

    if (quizState.isLoading && quizState.currentQuestion == null) {
        LoadingScreen(message = stringResource(id = R.string.quiz_loading_question))
    } else if (quizState.error != null) {
        ErrorScreen(message = quizState.error ?: "An unknown error occurred") {
            val online = com.aipoweredgita.app.utils.NetworkUtils.isNetworkAvailable(context)
            if (online) {
                viewModel.loadNextQuestion()
            } else {
                viewModel.setError("No Internet Connection\n\nPlease check your internet and try again.")
            }
        }
    } else if (quizState.currentQuestion != null) {
        val question = quizState.currentQuestion ?: return

        // Safety check: Ensure MCQ questions have options
        val isOpenEnded = question.type == com.aipoweredgita.app.data.QuestionType.ESSAY ||
                question.type == com.aipoweredgita.app.data.QuestionType.APPLICATION

        if (!isOpenEnded && question.options.isEmpty()) {
            ErrorScreen(message = "No answer options available for this question. Please try the next question.") {
                viewModel.loadNextQuestion()
            }
            return
        }

        if (!isOpenEnded && (question.correctAnswerIndex < 0 || question.correctAnswerIndex >= question.options.size)) {
            ErrorScreen(message = "Invalid question configuration. Please try the next question.") {
                viewModel.loadNextQuestion()
            }
            return
        }

        QuizContent(
            question = question.question,
            answer = buildString {
                // Show the correct answer text first
                if (question.options.isNotEmpty() && question.correctAnswerIndex >= 0) {
                    append("Correct Answer: ")
                    append(question.options[question.correctAnswerIndex])
                }
                // Add explanation if available
                if (!question.explanation.isNullOrBlank()) {
                    append("\n\nExplanation: ")
                    append(question.explanation)
                }
            },
            options = question.options,
            correctIndex = question.correctAnswerIndex,
            selectedIndex = quizState.selectedAnswerIndex,
            questionType = question.type,
            onSelect = { index ->
                viewModel.selectAnswer(index)
            },
            onSubmitAnswer = { answerText ->
                viewModel.submitOpenEndedAnswer(answerText)
            },
            onProceed = { wasCorrect ->
                if (!quizState.isQuizComplete) {
                    viewModel.loadNextQuestion()
                } else {
                    // Quiz is complete, don't load next question
                    // The completion dialog will be shown below
                }
            },
            vm = viewModel
        )
    }
    
    // Show completion dialog when quiz is complete
    if (quizState.isQuizComplete) {
        CompletionDialog(
            score = quizState.score,
            total = quizState.totalQuestions,
            onExit = onExitQuiz,
            onRestart = { 
                viewModel.restartQuiz() 
            }
        )
    }
}

