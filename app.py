import os
from flask import Flask, request, jsonify
from transformers import BertTokenizer, BertForSequenceClassification
import torch

app = Flask(__name__)

# 모델 경로 설정
current_dir = os.path.dirname(os.path.abspath(__file__))
MODEL_PATH = os.path.join(current_dir, "finance_sentiment_model")

# 모델 및 토크나이저 로드
try:
    tokenizer = BertTokenizer.from_pretrained(MODEL_PATH)
    model = BertForSequenceClassification.from_pretrained(MODEL_PATH)
    model.eval()  # 평가 모드로 설정
except Exception as e:
    print(f"Error loading model: {e}")
    exit(1)

@app.route('/analyze', methods=['GET'])
def analyze():
    try:
        text = request.args.get('text', '')
        if not text:
            return jsonify({'error': 'No text provided'}), 400

        # 감성 분석 수행
        inputs = tokenizer(text, return_tensors="pt", truncation=True, padding=True, max_length=128)
        outputs = model(**inputs)

        # logits 출력 확인
        logits = outputs.logits
        print(f"Model logits shape: {logits.shape}")
        print(f"Model logits values: {logits}")

        # logits 차원 검증
        if logits.shape[1] != 5:
            raise ValueError(f"Unexpected number of logits: {logits.shape[1]}. Expected 5.")

        # Softmax로 확률 계산
        probabilities = torch.nn.functional.softmax(logits, dim=1)[0]

        # 감정 예측
        sentiment_index = torch.argmax(probabilities).item()
        sentiment_labels = ["Very Negative", "Negative", "Neutral", "Positive", "Very Positive"]
        sentiment_label = sentiment_labels[sentiment_index]

        # 결과 레이블과 확률 매핑
        results = {label: round(prob.item() * 100, 2) for label, prob in zip(sentiment_labels, probabilities)}

        print(f"Predicted sentiment index: {sentiment_index}")
        print(f"Sentiment probabilities: {results}")

        # 최종 응답 반환
        return jsonify({
            'sentiment': sentiment_label,
            'results': results
        })

    except Exception as e:
        print(f"Error: {e}")
        return jsonify({'error': str(e)}), 500

if __name__ == '__main__':
    print(f"Using model path: {MODEL_PATH}")
    app.run(host='0.0.0.0', port=5000, debug=True)
