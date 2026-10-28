"""Breaker: deterministic adversarial game-rule checks."""
from server.game_rules import Player, simulate_attack
def run():
    problems = []
    attacker = Player("BREAKER", 15, 100)
    for level in range(1, 60):
        victim = Player("VICTIM", level, 1000)
        for success in (False, True):
            result = simulate_attack(attacker, victim, True, success)
            if result["victim_reputation_after"] < 0:
                problems.append(f"Negative reputation at level {level}")
            if level <= 5 and result["victim_reputation_lost"]:
                problems.append(f"Beginner lost XP at level {level}")
            if not success and result["victim_reputation_lost"]:
                problems.append(f"Failed attack removed XP at level {level}")
    return problems
