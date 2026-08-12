"""Generate Weka ARFF from Malaysia Crop Area & Production by State (DOSM/DOA, 2017–2022).

Source CSV: src/main/resources/ml/crops_state_dataset.csv
Columns: state, date, crop_type, planted_area (Ha), production (Mt)

crop_type is mapped onto the app catalog:
  paddy        -> Paddy
  vegetables   -> Tomato, Lettuce, Chili
  fruits       -> Durian, Papaya
  cash_crops   -> Corn

Farm features (soil/water/fertilizer/budget/land) are sampled so J48 learns
state production priors + resource fit (classification, not fixed rules only).
"""
from __future__ import annotations

import csv
import random
from collections import Counter, defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "src" / "main" / "resources" / "ml"
DATASET = ROOT / "crops_state_dataset.csv"
# Fallback if the renamed file is missing
DATASET_FALLBACK = ROOT / "crops_state.csv"

STATES = [
    "Johor", "Kedah", "Kelantan", "Melaka", "Negeri Sembilan", "Pahang",
    "Perak", "Perlis", "Pulau Pinang", "Sabah", "Sarawak", "Selangor",
    "Terengganu", "Wilayah Persekutuan",
]
SOILS = ["Clay", "Loam", "Sandy", "Silty"]
CROPS = ["Tomato", "Lettuce", "Chili", "Corn", "Paddy", "Papaya", "Durian"]

# Official crop_type -> catalog crops with share of that type's production.
TYPE_MAP: dict[str, list[tuple[str, float]]] = {
    "paddy": [("Paddy", 1.0)],
    "vegetables": [("Tomato", 0.40), ("Lettuce", 0.35), ("Chili", 0.25)],
    "fruits": [("Durian", 0.55), ("Papaya", 0.45)],
    "cash_crops": [("Corn", 1.0)],
}

RAIN = {
    "Johor": 2, "Kedah": 1, "Kelantan": 2, "Melaka": 1, "Negeri Sembilan": 1,
    "Pahang": 2, "Perak": 2, "Perlis": 0, "Pulau Pinang": 1, "Sabah": 2,
    "Sarawak": 2, "Selangor": 1, "Terengganu": 2, "Wilayah Persekutuan": 1,
}


def q(value: str) -> str:
    return f"'{value}'" if " " in value else value


def write_arff(path: Path, relation: str, attrs: list[str], rows: list[str]) -> None:
    lines = [f"@relation {relation}", ""] + attrs + ["", "@data"] + rows
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(path.name, len(rows))


def norm_state(raw: str) -> str | None:
    state = (raw or "").strip()
    if state in ("Malaysia",):
        return None
    if state.startswith("W.P") or "Kuala Lumpur" in state or "Labuan" in state or "Putrajaya" in state:
        return "Wilayah Persekutuan"
    return state if state in STATES else None


def load_state_priors() -> dict[str, dict[str, float]]:
    """state -> crop -> strength from production (Mt) + planted area (Ha)."""
    path = DATASET if DATASET.is_file() else DATASET_FALLBACK
    if not path.is_file():
        raise FileNotFoundError(f"Missing dataset: {DATASET} or {DATASET_FALLBACK}")

    # Aggregate across years so rare years don't wipe a state.
    type_prod: dict[tuple[str, str], float] = defaultdict(float)
    type_area: dict[tuple[str, str], float] = defaultdict(float)
    with open(path, newline="", encoding="utf-8") as handle:
        for row in csv.DictReader(handle):
            state = norm_state(row["state"])
            crop_type = (row.get("crop_type") or "").strip().lower()
            if not state or crop_type not in TYPE_MAP:
                continue
            try:
                production = float(row.get("production") or 0)
                planted = float(row.get("planted_area") or 0)
            except ValueError:
                continue
            # Flower units are not comparable Mt; already excluded by TYPE_MAP.
            type_prod[(state, crop_type)] += max(0.0, production)
            type_area[(state, crop_type)] += max(0.0, planted)

    priors: dict[str, dict[str, float]] = {state: {crop: 0.0 for crop in CROPS} for state in STATES}
    for state in STATES:
        for crop_type, shares in TYPE_MAP.items():
            # Blend production (primary) with planted area (scale differs; dampen area).
            strength = type_prod[(state, crop_type)] + 0.15 * type_area[(state, crop_type)]
            if strength <= 0:
                continue
            for crop, share in shares:
                priors[state][crop] += strength * share

        # Floor so every catalog crop can appear somewhere.
        for crop in CROPS:
            if priors[state][crop] <= 0:
                priors[state][crop] = 1.0

        # Dampen extreme types (paddy often >80%) so J48 still learns vegetables/fruits/corn.
        for crop in CROPS:
            priors[state][crop] = priors[state][crop] ** 0.55

        total = sum(priors[state].values())
        for crop in CROPS:
            priors[state][crop] /= total

    return priors


def resource_fit(crop: str, soil: str, water: float, fertilizer: float, budget: float, land: float) -> float:
    """Mild multipliers so J48 also splits on farm resources."""
    fit = 1.0
    if crop == "Paddy":
        fit *= 1.6 if water >= 200 and soil in ("Clay", "Loam") else 0.45
        fit *= 1.2 if land >= 4 else 0.7
    elif crop == "Durian":
        fit *= 1.5 if budget >= 10000 and land >= 4.5 else 0.5
        fit *= 1.2 if soil in ("Loam", "Clay") else 0.7
        fit *= 1.1 if 90 <= water <= 180 else 0.8
    elif crop == "Papaya":
        fit *= 1.35 if 100 <= water <= 210 else 0.65
        fit *= 1.2 if soil in ("Sandy", "Loam") else 0.85
        fit *= 1.15 if 2.5 <= land <= 8 else 0.8
    elif crop == "Corn":
        fit *= 1.4 if land >= 5 and water >= 140 else 0.55
        fit *= 1.15 if fertilizer >= 12 else 0.85
    elif crop == "Chili":
        fit *= 1.45 if soil == "Sandy" else 0.75
        fit *= 1.2 if 90 <= water <= 210 else 0.8
    elif crop == "Lettuce":
        fit *= 1.5 if water < 140 or soil == "Silty" else 0.7
        fit *= 1.2 if land <= 5 else 0.85
    else:  # Tomato
        fit *= 1.3 if soil in ("Loam", "Clay") and water >= 130 else 0.75
        fit *= 1.15 if fertilizer >= 10 else 0.85
    return max(0.2, fit)


def sample_features() -> tuple[str, float, float, float, float]:
    regime = random.randrange(5)
    if regime == 0:  # paddy-like
        return (
            random.choice(["Clay", "Loam"]),
            round(random.uniform(210, 340), 1),
            round(random.uniform(10, 30), 1),
            round(random.uniform(6000, 16000), 1),
            round(random.uniform(4.0, 12.0), 1),
        )
    if regime == 1:  # orchard / durian-like
        return (
            random.choice(["Loam", "Clay"]),
            round(random.uniform(100, 170), 1),
            round(random.uniform(14, 35), 1),
            round(random.uniform(11000, 22000), 1),
            round(random.uniform(4.5, 11.0), 1),
        )
    if regime == 2:  # dry / leafy
        return (
            random.choice(["Silty", "Loam", "Sandy"]),
            round(random.uniform(70, 130), 1),
            round(random.uniform(5, 16), 1),
            round(random.uniform(3000, 10000), 1),
            round(random.uniform(1.5, 5.0), 1),
        )
    if regime == 3:  # sandy chili / papaya
        return (
            random.choice(["Sandy", "Loam"]),
            round(random.uniform(100, 200), 1),
            round(random.uniform(8, 24), 1),
            round(random.uniform(5000, 14000), 1),
            round(random.uniform(2.5, 7.5), 1),
        )
    # mid tomato / corn
    return (
        random.choice(["Loam", "Clay", "Silty"]),
        round(random.uniform(140, 220), 1),
        round(random.uniform(10, 28), 1),
        round(random.uniform(7000, 15000), 1),
        round(random.uniform(3.5, 9.0), 1),
    )


def choose_label(priors: dict[str, float], soil: str, water: float, fertilizer: float,
                 budget: float, land: float) -> str:
    scores = {
        crop: max(1e-9, priors[crop] * resource_fit(crop, soil, water, fertilizer, budget, land)
                  * random.uniform(0.92, 1.08))
        for crop in CROPS
    }
    # Weighted draw (not pure argmax) so minority catalog crops still appear in ARFF.
    total = sum(scores.values())
    pick = random.random() * total
    running = 0.0
    for crop, score in scores.items():
        running += score
        if pick <= running:
            return crop
    return max(scores, key=scores.get)


def top_states_for(priors: dict[str, dict[str, float]], crop: str, n: int = 5) -> list[str]:
    ranked = sorted(STATES, key=lambda s: priors[s][crop], reverse=True)
    return ranked[:n]


def main() -> None:
    random.seed(42)
    priors = load_state_priors()
    print("Loaded DOSM state priors from", DATASET.name if DATASET.is_file() else DATASET_FALLBACK.name)
    print("Example Selangor prior:", {k: round(v, 3) for k, v in priors["Selangor"].items()})
    print("Example Kedah prior:", {k: round(v, 3) for k, v in priors["Kedah"].items()})
    print("Example Johor prior:", {k: round(v, 3) for k, v in priors["Johor"].items()})

    state_nom = "{" + ",".join(q(state) for state in STATES) + "}"
    soil_nom = "{" + ",".join(SOILS) + "}"
    crop_nom = "{" + ",".join(CROPS) + "}"

    crop_rows: list[str] = []
    quotas: Counter[str] = Counter()

    # Phase 1: DOSM-informed natural samples per state.
    samples_per_state = 36
    for state in STATES:
        for _ in range(samples_per_state):
            soil, water, fertilizer, budget, land = sample_features()
            label = choose_label(priors[state], soil, water, fertilizer, budget, land)
            quotas[label] += 1
            npk = RAIN[state]
            crop_rows.append(
                f"{q(state)},{soil},{water},{fertilizer},{budget},{land},{npk},{label}"
            )

    # Phase 2: balance classes so J48 can learn all catalog crops (still from top DOSM states).
    per_crop_target = 130
    for crop in CROPS:
        need = max(0, per_crop_target - quotas[crop])
        states = top_states_for(priors, crop)
        for _ in range(need):
            state = random.choice(states)
            # Bias features toward this crop's fit profile.
            if crop == "Paddy":
                soil, water, fertilizer, budget, land = (
                    random.choice(["Clay", "Loam"]),
                    round(random.uniform(220, 340), 1),
                    round(random.uniform(12, 28), 1),
                    round(random.uniform(7000, 15000), 1),
                    round(random.uniform(4.5, 12.0), 1),
                )
            elif crop == "Durian":
                soil, water, fertilizer, budget, land = (
                    random.choice(["Loam", "Clay"]),
                    round(random.uniform(110, 165), 1),
                    round(random.uniform(15, 32), 1),
                    round(random.uniform(12000, 22000), 1),
                    round(random.uniform(5.0, 11.0), 1),
                )
            elif crop == "Papaya":
                soil, water, fertilizer, budget, land = (
                    random.choice(["Loam", "Sandy"]),
                    round(random.uniform(115, 195), 1),
                    round(random.uniform(10, 24), 1),
                    round(random.uniform(6500, 14000), 1),
                    round(random.uniform(2.8, 7.0), 1),
                )
            elif crop == "Corn":
                soil, water, fertilizer, budget, land = (
                    random.choice(["Loam", "Clay"]),
                    round(random.uniform(150, 210), 1),
                    round(random.uniform(14, 30), 1),
                    round(random.uniform(7500, 15000), 1),
                    round(random.uniform(5.5, 12.0), 1),
                )
            elif crop == "Chili":
                soil, water, fertilizer, budget, land = (
                    "Sandy",
                    round(random.uniform(100, 190), 1),
                    round(random.uniform(10, 24), 1),
                    round(random.uniform(5000, 12000), 1),
                    round(random.uniform(2.5, 6.5), 1),
                )
            elif crop == "Lettuce":
                soil, water, fertilizer, budget, land = (
                    random.choice(["Silty", "Loam"]),
                    round(random.uniform(70, 125), 1),
                    round(random.uniform(6, 16), 1),
                    round(random.uniform(3000, 10000), 1),
                    round(random.uniform(1.5, 4.5), 1),
                )
            else:
                soil, water, fertilizer, budget, land = (
                    random.choice(["Loam", "Clay"]),
                    round(random.uniform(140, 210), 1),
                    round(random.uniform(12, 26), 1),
                    round(random.uniform(7000, 13000), 1),
                    round(random.uniform(3.5, 6.5), 1),
                )
            quotas[crop] += 1
            npk = RAIN[state]
            crop_rows.append(
                f"{q(state)},{soil},{water},{fertilizer},{budget},{land},{npk},{crop}"
            )

    # Cap any class that still dominates after balancing (usually Paddy).
    max_per_crop = int(per_crop_target * 1.15)
    capped: list[str] = []
    seen: Counter[str] = Counter()
    random.shuffle(crop_rows)
    for row in crop_rows:
        label = row.rsplit(",", 1)[-1]
        if seen[label] >= max_per_crop:
            continue
        seen[label] += 1
        capped.append(row)
    crop_rows = capped
    quotas = seen

    random.shuffle(crop_rows)
    write_arff(
        ROOT / "crop_recommend.arff",
        "crop_recommend_dosm",
        [
            f"@attribute location {state_nom}",
            f"@attribute soil {soil_nom}",
            "@attribute water numeric",
            "@attribute fertilizerStock numeric",
            "@attribute budget numeric",
            "@attribute land numeric",
            "@attribute npkBand numeric",
            f"@attribute crop {crop_nom}",
        ],
        crop_rows,
    )
    print("crop class counts:", dict(quotas))

    # Fertilizer / grade still trained as classifiers with the same crop vocabulary.
    plan_rows: list[str] = []
    plan_counts: Counter[str] = Counter()
    for state in STATES:
        for soil in SOILS:
            for crop in CROPS:
                for _ in range(3):
                    water = round(random.uniform(60, 340), 1)
                    fertilizer = round(random.uniform(3, 40), 1)
                    score = 0
                    if fertilizer < 8:
                        score += 3
                    elif fertilizer < 15:
                        score += 2
                    elif fertilizer < 25:
                        score += 1
                    else:
                        score -= 1
                    if soil == "Sandy":
                        score += 2
                    if crop in ("Tomato", "Corn", "Chili", "Paddy", "Durian"):
                        score += 1
                    if crop == "Paddy" and water < 200:
                        score += 1
                    if crop == "Durian":
                        score += 1
                    if water < 110:
                        score += 1
                    plan = "Low" if score <= 1 else ("Medium" if score <= 3 else "High")
                    plan_counts[plan] += 1
                    plan_rows.append(f"{q(state)},{soil},{crop},{water},{fertilizer},{plan}")

    write_arff(
        ROOT / "fertilizer_plan.arff",
        "fertilizer_plan",
        [
            f"@attribute location {state_nom}",
            f"@attribute soil {soil_nom}",
            f"@attribute crop {crop_nom}",
            "@attribute water numeric",
            "@attribute fertilizerStock numeric",
            "@attribute plan {Low,Medium,High}",
        ],
        plan_rows,
    )
    print("fertilizer plan counts:", dict(plan_counts))

    grade_rows: list[str] = []
    grade_counts: Counter[str] = Counter()
    for state in STATES:
        for soil in SOILS:
            for crop in CROPS:
                for band in ("Low", "Medium", "High"):
                    for _ in range(2):
                        water = round(random.uniform(50, 350), 1)
                        fertilizer = round(random.uniform(2, 45), 1)
                        budget = round(random.uniform(2000, 22000), 1)
                        resource_score = (water / 200 + fertilizer / 20 + budget / 10000) / 3
                        band_n = {"Low": 0, "Medium": 1, "High": 2}[band]
                        soil_n = {"Loam": 1.0, "Silty": 0.9, "Clay": 0.85, "Sandy": 0.75}[soil]
                        score = (
                            resource_score * 0.50
                            + band_n / 2 * 0.30
                            + soil_n * 0.15
                            + RAIN[state] * 0.04
                            + random.uniform(-0.12, 0.12)
                        )
                        if score >= 1.05:
                            grade = "S"
                        elif score >= 0.88:
                            grade = "A"
                        elif score >= 0.70:
                            grade = "B"
                        elif score >= 0.52:
                            grade = "C"
                        elif score >= 0.36:
                            grade = "D"
                        else:
                            grade = "F"
                        grade_counts[grade] += 1
                        grade_rows.append(
                            f"{q(state)},{soil},{crop},{water},{fertilizer},{budget},{band},{grade}"
                        )

    write_arff(
        ROOT / "grade_predict.arff",
        "grade_predict",
        [
            f"@attribute location {state_nom}",
            f"@attribute soil {soil_nom}",
            f"@attribute crop {crop_nom}",
            "@attribute water numeric",
            "@attribute fertilizerStock numeric",
            "@attribute budget numeric",
            "@attribute expectedProfitBand {Low,Medium,High}",
            "@attribute grade {S,A,B,C,D,F}",
        ],
        grade_rows,
    )
    print("grade counts:", dict(grade_counts))
    print("Done. Delete data/ml/*.model (+ *.meta) so J48 retrains on next app launch.")


if __name__ == "__main__":
    main()
