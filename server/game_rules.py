"""GeoHack game rules: simulation only; no real-device access."""
from dataclasses import dataclass

@dataclass(frozen=True)
class Player:
    alias: str
    level: int
    reputation: int

def loss_percent(level: int) -> int:
    if level <= 5: return 0
    if level <= 10: return 5
    if level <= 20: return 10
    if level <= 40: return 15
    return 20

def simulate_attack(attacker: Player, victim: Player, encountered: bool, success: bool):
    if not encountered:
        raise PermissionError("A verified physical encounter is required")
    loss = victim.reputation * loss_percent(victim.level) // 100 if success else 0
    return {
        "attacker": attacker.alias,
        "victim": victim.alias,
        "simulation": True,
        "success": success,
        "victim_reputation_after": victim.reputation - loss,
        "victim_reputation_lost": loss,
        "attacker_xp_awarded": 50 if success else 0,
        "report": f"SIMULATED attack by {attacker.alias}: {'SUCCESS' if success else 'FAILED'}",
    }
