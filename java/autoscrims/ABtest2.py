import math
import subprocess
import sys
from argparse import ArgumentParser
from collections import Counter
from dataclasses import dataclass, field
from datetime import datetime
from multiprocessing.pool import ThreadPool
from pathlib import Path
from rich.console import Console
from rich.segment import Segments
from rich.table import Table
from rich.text import Text

@dataclass
class Match:
    map: str
    reverse: bool
    player1_wins: bool
    win_condition: str
    replay_name: str

    @property
    def win_condition_short(self) -> str:
        return {
           "The winning team painted enough of the map.": "Coverage",
            "The winning team destroyed all of the enemy team's units.": "Destroyed",
            "The winning team won on tiebreakers (painted more of the map).": "T Coverage",
            "The winning team won on tiebreakers (more towers alive).": "T Towers",
            "The winning team won on tiebreakers (more money).": "T Money",
            "The winning team won on tiebreakers (more paint stored in units).": "T Paint",
            "The winning team won on tiebreakers (more robots alive).": "T Robots",
            "The winning team won arbitrarily (coin flip).": "T Coin Flip",
            "Other team has resigned. Congrats on scaring them I guess...": "Resignation",
        }.get(self.win_condition, self.win_condition)

@dataclass
class PlayerStatistics:
    wins: int = 0
    win_maps: int = 0
    draw_maps: int = 0
    lose_maps: int = 0
    wins_by_condition: Counter = field(default_factory=lambda: Counter())

@dataclass
class State:
    player1: str
    player2: str
    maps: list[str]
    matches: list[Match]
    console: Console
    timestamp: str

    def print(self) -> None:
        maps_table = self.get_maps_table()
        stats_table = self.get_stats_table()

        maps_segments = list(maps_table.__rich_console__(self.console, self.console.options))
        stats_segments = list(stats_table.__rich_console__(self.console, self.console.options))

        maps_footer = maps_segments[-2].text
        stats_header = stats_segments[0].text

        separator = "┢"

        separator_length = max(len(maps_footer), len(stats_header))
        for i in range(1, separator_length):
            has_next = i < separator_length - 1
            has_up = i < len(maps_footer) and maps_footer[i] in "┴┘"
            has_down = i < len(stats_header) and stats_header[i] in "┳┓"

            if has_next and not has_up and not has_down:
                separator += "━"
            elif has_next and has_up and not has_down:
                separator += "┷"
            elif has_next and not has_up and has_down:
                separator += "┳"
            elif has_next and has_up and has_down:
                separator += "╈"
            elif not has_next and has_up and not has_down:
                separator += "┙"
            elif not has_next and not has_up and has_down:
                separator += "┓"
            elif not has_next and has_up and has_down:
                separator += "┪"

        self.console.print(Segments([*maps_segments[:-2], Text(separator, style="bold"), *stats_segments[2:]]))

    def get_maps_table(self) -> Table:
        table = Table()

        column_groups = 4
        for _ in range(column_groups):
            for column in ["Map", "1", "2"]:
                table.add_column(column, style="bold" if column == "Map" else None, overflow="fold")

        row_count = math.ceil(len(self.maps) / column_groups)
        for y in range(row_count):
            row = []

            for x in range(column_groups):
                map_idx = x * row_count + y
                if map_idx >= len(self.maps):
                    row.extend([""] * 3)
                    continue

                map = self.maps[map_idx]
                wins = 0
                losses = 0

                for reverse in [False, True]:
                    match = next((match for match in self.matches if match.map == map and match.reverse == reverse), None)
                    if match is None:
                        row.append("")
                    elif match.player1_wins != reverse:
                        # row.append(f"[green][link={self.get_viewer_url(match.replay_name)}]{match.win_condition_short}[/][/]")
                        row.append(f"[green]{match.win_condition_short}[/]")
                        wins += 1
                    else:
                        # row.append(f"[red][link={self.get_viewer_url(match.replay_name)}]{match.win_condition_short}[/][/]")
                        row.append(f"[red]{match.win_condition_short}[/]")
                        losses += 1

                if wins == 2:
                    map_color = "green"
                elif losses == 2:
                    map_color = "red"
                elif wins + losses == 2:
                    map_color = "yellow"
                else:
                    map_color = None

                map_prefix = f"[{map_color}]" if map_color is not None else ""
                map_suffix = "[/]" if map_color is not None else ""

                row.insert(-2, map_prefix + map + map_suffix)

            table.add_row(*row)

        return table

    def get_stats_table(self) -> Table:
        table = Table()

        table.add_column("Player", style="bold", overflow="fold")
        table.add_column("Results", overflow="fold")
        table.add_column("Win rate", overflow="fold")
        table.add_column("Maps", overflow="fold")

        player1_stats = PlayerStatistics()
        player2_stats = PlayerStatistics()

        for map in self.maps:
            wins = 0
            losses = 0

            for reverse in [False, True]:
                match = next((match for match in self.matches if match.map == map and match.reverse == reverse), None)
                if match is None:
                    continue

                if match.player1_wins != reverse:
                    wins += 1
                    player1_stats.wins += 1
                    player1_stats.wins_by_condition[match.win_condition_short] += 1
                else:
                    losses += 1
                    player2_stats.wins += 1
                    player2_stats.wins_by_condition[match.win_condition_short] += 1

            if wins == 2:
                player1_stats.win_maps += 1
                player2_stats.lose_maps += 1
            elif losses == 2:
                player1_stats.lose_maps += 1
                player2_stats.win_maps += 1
            elif wins + losses == 2:
                player1_stats.draw_maps += 1
                player2_stats.draw_maps += 1

        conditions = set(player1_stats.wins_by_condition.keys()) | set(player2_stats.wins_by_condition.keys())
        conditions = sorted(conditions, key=lambda c: player1_stats.wins_by_condition[c] + player2_stats.wins_by_condition[c], reverse=True)
        for condition in conditions:
            table.add_column(condition, overflow="fold")

        match_count = len(self.matches)

        for name, stats, other_stats in [
            (self.player1, player1_stats, player2_stats),
            (self.player2, player2_stats, player1_stats)
        ]:
            row = [
                name,
                f"[green]{stats.wins}[/]/[red]{match_count - stats.wins}[/]",
                f"{stats.wins / match_count * 100 if match_count > 0 else 0:,.2f}%",
                f"[green]{stats.win_maps}[/]/[yellow]{stats.draw_maps}[/]/[red]{stats.lose_maps}[/]"
            ]

            for condition in conditions:
                wins = stats.wins_by_condition[condition]
                losses = other_stats.wins_by_condition[condition]
                row.append(f"[green]{wins}[/]/[red]{losses}[/]")

            table.add_row(*row)

        return table

    def get_viewer_url(self, replay_name: str) -> str:
        return f"http://localhost:8000/client/resources/app/dist/index.html?gameSource=http://localhost:8000/{replay_name}"

def run_match(state: State, map: str, reverse: bool) -> None:
    player1 = state.player1 if not reverse else state.player2
    player2 = state.player2 if not reverse else state.player1

    replay_name = f"replays/run-{state.timestamp}-{player1}-vs-{player2}-on-{map}.bc24"

    process = subprocess.run([
        str(Path(__file__).parent.parent / "gradlew"),
        "run",
        "-x", "unpackClient",
        f"-PteamA={player1}",
        f"-PteamB={player2}",
        f"-Pmaps={map}",
        f"-PreplayPath={replay_name}"
    ], stdout=subprocess.PIPE, stderr=subprocess.STDOUT, cwd=str(Path(__file__).parent.parent))
    stdout = process.stdout.decode("utf-8")

    if process.returncode != 0:
        print(f"Gradle exited with status code {process.returncode} after running {player1} vs. {player2} on {map}\n{stdout}")
        sys.exit(process.returncode)

    lines = stdout.splitlines()

    winner_line = next(line for line in lines if ") wins (" in line)
    player1_wins = "(A)" in winner_line

    win_condition_line = next(line for line in lines if line.startswith("[server] Reason: "))
    win_condition = win_condition_line.split(": ", 1)[1]

    state.matches.append(Match(map, reverse, player1_wins, win_condition, replay_name))
    state.print()

def main() -> None:
    parser = ArgumentParser(description="Compare the performance of two players.")
    parser.add_argument("player1", type=str, help="name of the first player")
    parser.add_argument("player2", type=str, help="name of the second player")

    args = parser.parse_args()

    # build_proc = subprocess.run([str(Path(__file__).parent.parent / "gradlew"), "build", "-x", "checkForUpdates"])
    # if build_proc.returncode != 0:
    #     sys.exit(build_proc.returncode)

    # Based on ENGINE_BUILTIN_MAP_NAMES in https://github.com/battlecode/battlecode24/blob/master/client/src/constants.ts
    maps = """
      AlarmClock
      Barcode
      BatSignal
      Brat
      Bread
      Bunny
      BunnyGame
      Castle
      CastleDefense
      Circuit
      Crab
      DefaultLarge
      DefaultMedium
      DefaultSmall
      Dominoes
      DonkeyKong
      Filter
      Flower
      Fossil
      FourCorners
      Gears
      HungerGames
      Jail
      Justice
      Mirage
      Money
      MoneyTower
      Oasis
      Paintball
      Parking_lot
      Piglets2
      PlumberGame
      Portal
      Racetrack
      Restart
      Rose
      SaltyPepper
      SandyBeach
      Snowglobe
      Snowman
      TargetPractice
      Terminal
      TheBest
      Thirds
      UglySweater
      UnderTheSea
      box
      boxofchocolates
      catface
      defensetower
      fix
      galaxy
      gardenworld
      giver
      gridworld
      headphones
      leavemealone
      lighthouse
      maze
      memstore
      mit
      quack
      rain
      roads
      sayhi
      shell
      sierpinski
      starburst
      sunrise
      walalilongla
      windmill
      yearofthesnake""".split()
    

    console = Console(highlight=False)
    timestamp = datetime.now().strftime("%Y-%m-%d_%H-%M-%S")

    state = State(args.player1, args.player2, maps, [], console, timestamp)
    state.print()

    matches = []
    for map in maps:
        for reverse in [False, True]:
            matches.append((state, map, reverse))

    with ThreadPool(8) as pool:
        pool.starmap(run_match, matches)

if __name__ == "__main__":
    main()