import unittest
from server.game_rules import Player, simulate_attack, loss_percent

class GameRulesTests(unittest.TestCase):
    def setUp(self):
        self.attacker = Player("A", 12, 200)

    def test_beginner_protection(self):
        for level in range(1, 6):
            result = simulate_attack(self.attacker, Player("B", level, 1000), True, True)
            self.assertEqual(result["victim_reputation_lost"], 0)

    def test_experienced_player_loses_rep(self):
        result = simulate_attack(self.attacker, Player("B", 12, 1000), True, True)
        self.assertEqual(result["victim_reputation_lost"], 100)

    def test_encounter_required(self):
        with self.assertRaises(PermissionError):
            simulate_attack(self.attacker, Player("B", 12, 100), False, True)

    def test_failed_attack_no_rep_loss(self):
        result = simulate_attack(self.attacker, Player("B", 12, 1000), True, False)
        self.assertEqual(result["victim_reputation_lost"], 0)

    def test_level_boundaries(self):
        self.assertEqual([loss_percent(i) for i in [5,6,10,11,20,21,40,41]], [0,5,5,10,10,15,15,20])

if __name__ == "__main__":
    unittest.main()
