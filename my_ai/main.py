import numpy as np
import random
import pickle
import os

class Brain:
    def __init__(self, input_dim=2, output_dim=2):
        self.W = np.random.randn(input_dim, output_dim) * 0.1
        self.b = np.zeros(output_dim)

    def decide(self, state):
        logits = state @ self.W + self.b
        return int(np.argmax(logits))

    def get_weights(self):
        return (self.W.copy(), self.b.copy())

    def set_weights(self, W, b):
        self.W = W.copy()
        self.b = b.copy()

def train_skill(generations=30):
    population = [Brain() for _ in range(30)]
    best_brain = None
    best_score = -float('inf')

    for gen in range(generations):
        scores = []
        for brain in population:
            total = 0
            for _ in range(5):
                pos = 0.0
                target = random.uniform(-5, 5)
                for _ in range(50):
                    state = np.array([pos, target], dtype=np.float32)
                    action = brain.decide(state)
                    pos += 0.5 if action == 1 else -0.5
                    dist = abs(pos - target)
                    if dist < 0.5:
                        total += 100
                        break
                    elif abs(pos) > 10:
                        total -= 100
                        break
                    else:
                        total -= dist
            scores.append(total)

        idx = int(np.argmax(scores))
        if scores[idx] > best_score:
            best_score = scores[idx]
            best_brain = population[idx]

        sorted_idx = np.argsort(scores)[::-1]
        parents = [population[i] for i in sorted_idx[:10]]

        new_pop = []
        while len(new_pop) < 30:
            p1, p2 = random.choice(parents), random.choice(parents)
            W1, b1 = p1.get_weights()
            W2, b2 = p2.get_weights()
            mask = np.random.rand(*W1.shape) > 0.5
            W_child = np.where(mask, W1, W2) + np.random.randn(*W1.shape) * 0.1
            b_child = np.where(np.random.rand(*b1.shape) > 0.5, b1, b2) + np.random.randn(*b1.shape) * 0.1
            new_pop.append(Brain())
            new_pop[-1].set_weights(W_child, b_child)
        population = new_pop

        if (gen + 1) % 10 == 0:
            print(f"第 {gen+1} 代 | 最高分: {best_score:.1f}")

    return best_brain

if __name__ == "__main__":
    skill_path = "skills/find_target.pkl"

    if os.path.exists(skill_path):
        print("发现已有技能，直接加载...")
        with open(skill_path, "rb") as f:
            W, b = pickle.load(f)
        brain = Brain()
        brain.set_weights(W, b)
    else:
        print("没有技能，开始从零训练...")
        brain = train_skill(generations=30)
        with open(skill_path, "wb") as f:
            pickle.dump(brain.get_weights(), f)
        print("训练完成，技能已保存到 skills/find_target.pkl")

    print("\n--- 测试 AI ---")
    pos = 0.0
    target = 3.0
    for step in range(20):
        state = np.array([pos, target], dtype=np.float32)
        action = brain.decide(state)
        pos += 0.5 if action == 1 else -0.5
        print(f"步 {step}: 位置 {pos:.1f}, 目标 {target:.1f}, 动作 {'向右' if action==1 else '向左'}")
        if abs(pos - target) < 0.5:
            print("找到目标了！")
            break
