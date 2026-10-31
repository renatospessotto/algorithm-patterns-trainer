"""Testes do ETL com fixtures sinteticas (fixtures/): nenhum dado real, sem rede."""
import json
import os
import tempfile
import unittest

import generate_seed as etl

FIXTURES = os.path.join(os.path.dirname(os.path.abspath(__file__)), "fixtures")


def load_fixture():
    with open(os.path.join(FIXTURES, "questions.json"), encoding="utf-8") as f:
        questions = json.load(f)["questions"]
    ratings = etl.load_ratings(os.path.join(FIXTURES, "ratings.txt"))
    return questions, ratings


class LoadRatingsTest(unittest.TestCase):
    def test_parses_numeric_ratings_and_skips_invalid_rows(self):
        _, ratings = load_fixture()
        self.assertEqual(ratings, {"fake-grid-paths": 1500.12})


class ClassifyPatternsTest(unittest.TestCase):
    def test_first_matching_tag_is_primary(self):
        self.assertEqual(
            etl.classify_patterns(["Stack", "Sliding Window"]),
            [{"slug": "sliding-window", "is_primary": True}, {"slug": "monotonic-stack", "is_primary": False}],
        )

    def test_dp_with_matrix_is_promoted_to_2d(self):
        self.assertEqual(
            etl.classify_patterns(["Dynamic Programming", "Matrix"]),
            [{"slug": "dfs-bfs-graphs-matrices", "is_primary": True}, {"slug": "dp-2d-strings", "is_primary": False}],
        )

    def test_generic_tags_map_to_nothing(self):
        self.assertEqual(etl.classify_patterns(["Array", "Sorting", "Math"]), [])


class TransformTest(unittest.TestCase):
    def setUp(self):
        questions, ratings = load_fixture()
        self.problems = {p["external_slug"]: p for p in etl.transform(questions, ratings, require_all_core=False)}

    def test_deduplicates_by_slug_keeping_first(self):
        self.assertEqual(len(self.problems), 5)
        self.assertEqual(self.problems["fake-hash-lookup"]["title"], "Fake Hash Lookup")

    def test_merges_rating_by_slug(self):
        self.assertEqual(self.problems["fake-grid-paths"]["difficulty_rating"], 1500.12)
        self.assertIsNone(self.problems["fake-hash-lookup"]["difficulty_rating"])

    def test_core_problem_gets_its_curated_pattern_even_without_tags(self):
        core = self.problems["two-sum"]
        self.assertTrue(core["is_core"])
        self.assertEqual(core["core_pattern_slug"], "hashmap-set")
        self.assertEqual(core["core_order"], 0)
        self.assertEqual(core["patterns"], [{"slug": "hashmap-set", "is_primary": True}])

    def test_unmapped_problem_stays_in_general_pool(self):
        plain = self.problems["fake-plain-array"]
        self.assertFalse(plain["is_core"])
        self.assertEqual(plain["patterns"], [])
        self.assertEqual(plain["leetcode_url"], "https://leetcode.com/problems/fake-plain-array/")

    def test_missing_core_problems_fail_by_default(self):
        questions, ratings = load_fixture()
        with self.assertRaises(SystemExit):
            etl.transform(questions, ratings)


class LoadTest(unittest.TestCase):
    def test_writes_both_seed_files(self):
        questions, ratings = load_fixture()
        problems = etl.transform(questions, ratings, require_all_core=False)
        with tempfile.TemporaryDirectory() as out:
            problems_path, topics_path = etl.load(problems, out)
            with open(problems_path, encoding="utf-8") as f:
                written = json.load(f)
            self.assertEqual(len(written["patterns"]), 14)
            self.assertEqual(len(written["problems"]), 5)
            with open(topics_path, encoding="utf-8") as f:
                self.assertEqual(json.load(f)["topics"], etl.TOPICS)


if __name__ == "__main__":
    unittest.main()
