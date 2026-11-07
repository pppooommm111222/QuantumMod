import numpy as np
import random

class Brain:
    def __init__(self, input_dim, output_dim):
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

def crossover(p1, p2):
    W1, b1 = p1.get_weights()
    W2, b2 = p2.get_weights()
    mask = np.random.rand(*W1.shape) > 0.5
    W = np.where(mask, W1, W2)
    b = np.where(np.random.rand(*b1.shape) > 0.5, b1, b2)
    return W, b

def mutate(W, b, rate=0.1):
    W = W + np.random.randn(*W.shape) * rate
    b = b + np.random.randn(*b.shape) * rate
    return W, b
