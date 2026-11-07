import numpy as np
import random
import pickle
import os
from brain import Brain, crossover, mutate

# ============================================================
# 1. 绕障环境（6 输入，4 输出）
# ============================================================
class AvoidEnv:
    def __init__(self):
        self.start = np.array([0.0, 0.0])
        self.target = np.array([5.0, 5.0])
        self.obstacle = np.array([2.5, 2.5])
        self.radius = 1.2
        self.agent = self.start.copy()

    def reset(self):
        self.agent = self.start.copy()
        return self.get_state()

    def get_state(self):
        return np.array([
            self.agent[0], self.agent[1],
            self.target[0], self.target[1],
            self.obstacle[0], self.obstacle[1]
        ], dtype=np.float32)

    def step(self, action):
        if action == 0:   self.agent[1] += 0.4   # 上
        elif action == 1: self.agent[1] -= 0.4   # 下
        elif action == 2: self.agent[0] -= 0.4   # 左
        elif action == 3: self.agent[0] += 0.4   # 右

        self.agent[0] = np.clip(self.agent[0], -1, 6)
        self.agent[1] = np.clip(self.agent[1], -1, 6)

        d_target = np.linalg.norm(self.agent - self.target)
        d_obstacle = np.linalg.norm(self.agent - self.obstacle)

        if d_obstacle < self.radius:
            return self.get_state(), -100, True
        if d_target < 0.5:
            return self.get_state(), 200, True
        return self.get_state(), -d_target * 0.1, False


# ============================================================
# 2. 关键：把走路 AI 的权重“移植”到绕障 AI 身上
# ============================================================
def seed_from_walk(walk_path):
    """
    走路 AI: 2 输入 [pos, target] -> 2 输出 [左, 右]
    绕障 AI: 6 输入 [ax, ay, tx, ty, ox, oy] -> 4 输出 [上, 下, 左, 右]

    移植逻辑：
    - 走路 AI 的输入 pos   -> 绕障 AI 的输入 ax（agent_x）
    - 走路 AI 的输入 target -> 绕障 AI 的输入 tx（target_x）
    - 走路 AI 的输出 左/右 -> 绕障 AI 的输出 左/右（动作 2 和 3）
    - 绕障 AI 的“上/下”两个动作，权重从随机小值开始学
    """
    with open(walk_path, "rb") as f:
        W_walk, b_walk = pickle.load(f)

    # 创建绕障 AI 的初始权重（6x4）
    W = np.random.randn(6, 4) * 0.02   # 整体先随机小值
    b = np.zeros(4)

    # --- 移植“水平方向朝目标走”的能力 ---
    # 走路AI: W_walk[0,0]=pos->左, W_walk[0,1]=pos->右
    #         W_walk[1,0]=target->左, W_walk[1,1]=target->右
    W[0, 2] = W_walk[0, 0]   # agent_x -> 左
    W[0, 3] = W_walk[0, 1]   # agent_x -> 右
    W[2, 2] = W_walk[1, 0]   # target_x -> 左
    W[2, 3] = W_walk[1, 1]   # target_x -> 右
    b[2] = b_walk[0]         # 左的偏置
    b[3] = b_walk[1]         # 右的偏置

    # --- 上下动作：也给它一点“向目标y靠近”的初始倾向 ---
    W[1, 0] = 0.1            # agent_y -> 上（如果 y 太小就向上）
    W[1, 1] = -0.1           # agent_y -> 下
    W[3, 0] = 0.1            # target_y -> 上
    W[3, 1] = -0.1           # target_y -> 下

    print("已把走路 AI 的能力移植到绕障 AI 的初始权重。")
    return W, b


# ============================================================
# 3. 带“播种”的进化训练
# ============================================================
def train_with_seed(env, W_seed, b_seed, generations=60, pop_size=50):
    input_dim, output_dim = W_seed.shape

    # 种群第一个个体 = 移植过来的大脑，其余随机
    population = []
    seed_brain = Brain(input_dim, output_dim)
    seed_brain.set_weights(W_seed, b_seed)
    population.append(seed_brain)

    for _ in range(pop_size - 1):
        population.append(Brain(input_dim, output_dim))

    best_brain = seed_brain
    best_score = -float('inf')

    for gen in range(generations):
        scores = []
        for brain in population:
            total = 0
            for _ in range(3):
                state = env.reset()
                for _ in range(60):
                    action = brain.decide(state)
                    state, reward, done = env.step(action)
                    total += reward
                    if done:
                        break
            scores.append(total)

        idx = int(np.argmax(scores))
        if scores[idx] > best_score:
            best_score = scores[idx]
            best_brain = population[idx]

        sorted_idx = np.argsort(scores)[::-1]
        parents = [population[i] for i in sorted_idx[:pop_size // 3]]

        new_pop = []
        while len(new_pop) < pop_size:
            p1, p2 = random.choice(parents), random.choice(parents)
            W, b = crossover(p1, p2)
            W, b = mutate(W, b, rate=0.05)
            child = Brain(input_dim, output_dim)
            child.set_weights(W, b)
            new_pop.append(child)

        population = new_pop

        if (gen + 1) % 10 == 0:
            print(f"  第 {gen+1:2d} 代 | 最高分: {best_score:8.1f}")

    return best_brain


# ============================================================
# 4. 主程序
# ============================================================
if __name__ == "__main__":
    walk_path = "skills/walk.pkl"
    avoid_path = "skills/avoid_seeded.pkl"

    if not os.path.exists(walk_path):
        print("找不到 skills/walk.pkl，请先训练走路 AI。")
        exit()

    if os.path.exists(avoid_path):
        print("发现已有移植版绕障技能，直接加载...")
        with open(avoid_path, "rb") as f:
            W, b = pickle.load(f)
        brain = Brain(6, 4)
        brain.set_weights(W, b)
    else:
        print("=" * 50)
        print("阶段1：加载走路 AI，并移植到绕障 AI 的初始权重")
        print("=" * 50)
        W_seed, b_seed = seed_from_walk(walk_path)

        print()
        print("=" * 50)
        print("阶段2：带着走路底子，从零学绕障")
        print("=" * 50)
        brain = train_with_seed(AvoidEnv(), W_seed, b_seed, generations=60, pop_size=50)

        with open(avoid_path, "wb") as f:
            pickle.dump(brain.get_weights(), f)
        print("移植版绕障技能已保存到 skills/avoid_seeded.pkl")

    # ========================================================
    # 5. 测试
    # ========================================================
    print()
    print("=" * 50)
    print("测试：移植版 AI 走一遍")
    print("=" * 50)

    env = AvoidEnv()
    state = env.reset()

    for step in range(50):
        action = brain.decide(state)
        state, reward, done = env.step(action)
        arrow = ["上", "下", "左", "右"][action]
        print(f"步 {step:2d} | 位置 ({env.agent[0]:.1f}, {env.agent[1]:.1f}) | 动作 {arrow} | 奖励 {reward:.1f}")

        if done:
            if np.linalg.norm(env.agent - env.target) < 0.5:
                print("✅ 成功到达目标！")
            else:
                print("❌ 撞到障碍物或出界。")
            break
