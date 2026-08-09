package evaluation.evaluationService.evaluation.adapter.out;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import evaluation.evaluationService.evaluation.application.port.out.EvaluateRecoveryPort;
import evaluation.evaluationService.evaluation.domain.model.vo.EvaluationResult;
import evaluation.evaluationService.evaluation.domain.model.vo.RecoveryCase;
import evaluation.evaluationService.evaluation.domain.model.vo.RetrievedCase;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class GeminiEvaluator implements EvaluateRecoveryPort {

    private final JudgeAiService judgeService;


    public GeminiEvaluator(ChatModel chatModel) {
        this.judgeService = AiServices.create(JudgeAiService.class, chatModel);
    }

    interface JudgeAiService {
        @SystemMessage("You are a strict evaluator for YouTube playlist recoveries. You must return ONLY a JSON object with 'label' (SUCCESS, ACCEPTABLE, or FAIL), 'confidence' (float), and 'reason' (string).")
        @UserMessage("""
                Target video: {{target}}
                Recovered video: {{source}}
                
                SUCCESS: The recovered video is an appropriate replacement.
                ACCEPTABLE: The recovered video is related and usable, but is not an ideal replacement.
                FAIL: The recovered video is not an appropriate replacement.
                """)
        EvaluationResult evaluateZeroShot(
                @V("target") String target,
                @V("source") String source
        );

        @SystemMessage("You are a strict evaluator for YouTube playlist recoveries. You must return ONLY a JSON object with 'label' (SUCCESS, ACCEPTABLE, or FAIL), 'confidence' (float), and 'reason' (string).")
        @UserMessage("""
                Current recovery:
                Target video: {{target}}
                Recovered video: {{source}}
                
                Below are previous recovery cases that were reviewed by humans.
                Use them as reference examples, but make your own judgment.
                
                {{examples}}
                """)
        EvaluationResult evaluateWithRag(
                @V("target") String target,
                @V("source") String source,
                @V("examples") String examples
        );
    }

    public EvaluationResult evaluateZeroShot(RecoveryCase testCase) {
        return judgeService.evaluateZeroShot(
                testCase.targetTitle(),
                testCase.sourceTitle()
        );
    }

    public EvaluationResult evaluateWithRag(RecoveryCase testCase, List<RetrievedCase> similarCases) {
        String examples = similarCases.stream()
                .map(this::formatExample)
                .collect(Collectors.joining("\n\n"));

        return judgeService.evaluateWithRag(
                testCase.targetTitle(),
                testCase.sourceTitle(),
                examples
        );
    }

    private String formatExample(RetrievedCase retrievedCase) {
        RecoveryCase example = retrievedCase.recoveryCase();
        return """
                Target: %s
                Source: %s
                Human Label: %s
                Human Reason: %s
                """.formatted(
                example.targetTitle(),
                example.sourceTitle(),
                example.humanLabel(),
                example.humanReason()
        );
    }
}