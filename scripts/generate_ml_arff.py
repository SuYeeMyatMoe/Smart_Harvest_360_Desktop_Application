"""Generate Malaysia-informed Weka ARFF datasets under src/main/resources/ml/.

Crop labels use clear exclusive rules (synced with CropFeatureScorer.java)
including Tomato, Lettuce, Chili, Corn, Paddy, Papaya, Durian.
"""
from __future__ import annotations

import random
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "src" / "main" / "resources" / "ml"
STATES = [
    "Johor", "Kedah", "Kelantan", "Melaka", "Negeri Sembilan", "Pahang",
    "Perak", "Perlis", "Pulau Pinang", "Sabah", "Sarawak", "Selangor",
    "Terengganu", "Wilayah Persekutuan",
]
SOILS = ["Clay", "Loam", "Sandy", "Silty"]
CROPS = ["Tomato", "Lettuce", "Chili", "Corn", "Paddy", "Papaya", "Durian"]


def q(value: str) -> str:
    return f"'{value}'" if " " in value else value


def write_arff(path: Path, relation: str, attrs: list[str], rows: list[str]) -> None:
    lines = [f"@relation {relation}", ""] + attrs + ["", "@data"] + rows
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(path.name, len(rows))


def label_crop(soil: str, water: float, fertilizer: float, budget: float, land: float, state: str) -> str:
    """Exclusive priority rules — keep in sync with CropFeatureScorer.ruleLabel()."""
    if water >= 220 and land >= 4.0 and soil in ("Clay", "Loam"):
        return "Paddy"
    if land >= 6.5 and 160 <= water < 220 and fertilizer >= 14:
        return "Corn"
    if budget >= 12000 and land >= 5.0 and fertilizer >= 15 and 100 <= water < 160 and soil in ("Loam", "Clay"):
        return "Durian"
    if soil == "Sandy" and budget >= 4500 and water >= 85 and water < 210:
        return "Chili"
    if state in ("Johor", "Kelantan", "Terengganu", "Sabah", "Sarawak", "Pahang") and 110 <= water <= 200 and budget >= 6000 and 2.5 <= land <= 7.5 and soil != "Silty":
        return "Papaya"
    if soil == "Silty" or water < 125:
        return "Lettuce"
    if land <= 3.5 and water <= 180:
        return "Lettuce"
    if state in ("Kelantan", "Terengganu", "Johor", "Perlis") and 100 <= water <= 210 and budget >= 5000:
        return "Chili"
    if state in ("Kedah", "Perak", "Pahang", "Sarawak") and land >= 5.5 and water >= 140 and water < 220:
        return "Corn"
    if soil in ("Loam", "Clay") and water >= 140 and fertilizer >= 12:
        return "Tomato"
    if budget >= 9000 and water >= 150 and water < 220:
        return "Tomato"
    if fertilizer < 10:
        return "Lettuce"
    return "Tomato"


def force_features(target: str) -> tuple[str, str, float, float, float, float]:
    """Return state, soil, water, fertilizer, budget, land that label as target."""
    if target == "Paddy":
        return "Kedah", "Clay", 250.0, 18.0, 10000.0, 6.0
    if target == "Durian":
        return "Pahang", "Loam", 140.0, 18.0, 15000.0, 6.0
    if target == "Papaya":
        return "Johor", "Loam", 150.0, 14.0, 8000.0, 4.0
    if target == "Corn":
        return "Kedah", "Loam", 180.0, 18.0, 10000.0, 8.0
    if target == "Chili":
        return "Johor", "Sandy", 140.0, 14.0, 7000.0, 4.0
    if target == "Lettuce":
        return "Selangor", "Silty", 100.0, 8.0, 5000.0, 2.5
    return "Selangor", "Loam", 180.0, 16.0, 9000.0, 5.0


def sample_for_target(target: str) -> tuple[str, str, float, float, float, float]:
    if target == "Paddy":
        state = random.choice(["Kedah", "Perlis", "Kelantan", "Terengganu", "Perak", "Kedah"])
        soil = random.choice(["Clay", "Loam", "Clay"])
        water = round(random.uniform(225, 340), 1)
        fertilizer = round(random.uniform(12, 30), 1)
        budget = round(random.uniform(7000, 16000), 1)
        land = round(random.uniform(4.0, 12.0), 1)
    elif target == "Durian":
        state = random.choice(["Pahang", "Johor", "Negeri Sembilan", "Kelantan", "Terengganu", "Pahang"])
        soil = random.choice(["Loam", "Clay", "Loam"])
        water = round(random.uniform(110, 155), 1)
        fertilizer = round(random.uniform(16, 35), 1)
        budget = round(random.uniform(12500, 22000), 1)
        land = round(random.uniform(5.0, 10.0), 1)
    elif target == "Papaya":
        state = random.choice(["Johor", "Kelantan", "Terengganu", "Sabah", "Sarawak", "Pahang"])
        soil = random.choice(["Loam", "Sandy", "Loam"])
        water = round(random.uniform(115, 195), 1)
        fertilizer = round(random.uniform(10, 24), 1)
        budget = round(random.uniform(6200, 11000), 1)
        land = round(random.uniform(2.8, 6.5), 1)
    elif target == "Corn":
        state = random.choice(["Kedah", "Perak", "Pahang", "Sarawak", "Selangor"])
        soil = random.choice(["Loam", "Clay"])
        water = round(random.uniform(165, 210), 1)
        fertilizer = round(random.uniform(15, 35), 1)
        budget = round(random.uniform(8000, 11500), 1)
        land = round(random.uniform(6.5, 13.0), 1)
    elif target == "Chili":
        state = random.choice(["Johor", "Kelantan", "Terengganu", "Perlis", "Selangor"])
        soil = "Sandy"
        water = round(random.uniform(95, 190), 1)
        fertilizer = round(random.uniform(10, 26), 1)
        budget = round(random.uniform(5000, 14000), 1)
        land = round(random.uniform(2.5, 6.5), 1)
    elif target == "Lettuce":
        state = random.choice(STATES)
        soil = random.choice(["Silty", "Loam", "Silty"])
        water = round(random.uniform(70, 120), 1)
        fertilizer = round(random.uniform(5, 16), 1)
        budget = round(random.uniform(3000, 10000), 1)
        land = round(random.uniform(1.5, 3.5), 1)
    else:  # Tomato
        state = random.choice(["Selangor", "Melaka", "Negeri Sembilan", "Pulau Pinang", "Perak"])
        soil = random.choice(["Loam", "Clay"])
        water = round(random.uniform(145, 205), 1)
        fertilizer = round(random.uniform(13, 28), 1)
        budget = round(random.uniform(7500, 11500), 1)
        land = round(random.uniform(3.8, 6.2), 1)
    return state, soil, water, fertilizer, budget, land


def main() -> None:
    rain = {
        "Johor": 2, "Kedah": 1, "Kelantan": 2, "Melaka": 1, "Negeri Sembilan": 1,
        "Pahang": 2, "Perak": 2, "Perlis": 0, "Pulau Pinang": 1, "Sabah": 2,
        "Sarawak": 2, "Selangor": 1, "Terengganu": 2, "Wilayah Persekutuan": 1,
    }

    random.seed(42)
    state_nom = "{" + ",".join(q(state) for state in STATES) + "}"
    soil_nom = "{" + ",".join(SOILS) + "}"
    crop_nom = "{" + ",".join(CROPS) + "}"

    crop_rows: list[str] = []
    quotas: Counter[str] = Counter()
    per_crop = 120
    for target in CROPS:
        for _ in range(per_crop):
            state, soil, water, fertilizer, budget, land = sample_for_target(target)
            label = label_crop(soil, water, fertilizer, budget, land, state)
            if label != target:
                state, soil, water, fertilizer, budget, land = force_features(target)
                label = label_crop(soil, water, fertilizer, budget, land, state)
            quotas[label] += 1
            npk = rain[state]
            crop_rows.append(
                f"{q(state)},{soil},{water},{fertilizer},{budget},{land},{npk},{label}"
            )

    random.shuffle(crop_rows)
    write_arff(
        ROOT / "crop_recommend.arff",
        "crop_recommend",
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
    print("examples:")
    print("  default Loam:", label_crop("Loam", 200, 20, 10000, 5, "Selangor"))
    print("  high water Clay:", label_crop("Clay", 250, 18, 10000, 6, "Kedah"))
    print("  rich orchard:", label_crop("Loam", 140, 18, 15000, 6, "Pahang"))
    print("  warm papaya:", label_crop("Loam", 150, 14, 8000, 4, "Johor"))
    print("  sandy chili:", label_crop("Sandy", 140, 14, 7000, 4, "Johor"))
    print("  dry lettuce:", label_crop("Silty", 90, 8, 4000, 2, "Selangor"))

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
                    if crop == "Durian":
                        score += 1
                    if water < 110:
                        score += 1
                    if crop == "Paddy" and water < 200:
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
                            + rain[state] * 0.04
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


if __name__ == "__main__":
    main()
