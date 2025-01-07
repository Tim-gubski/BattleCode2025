# Battlecode 2025 Scaffold - Python

This is the Battlecode 2025 Python scaffold, containing an `examplefuncsplayer`. Read https://play.battlecode.org/bc25python/quick_start !


### Project Structure

- `README.md`
    This file.
- `run.py`
    The python script used to run players and upgrade versions.
- `src/`
    Player source code.
- `test/`
    Player test code.
- `client/`
    Contains the client. The proper executable can be found in this folder (don't move this!)
- `matches/`
    The output folder for match files.
- `maps/`
    The default folder for custom maps.

### How to get started

You are free to directly edit `examplefuncsplayer`.
However, we recommend you make a new bot by copying `examplefuncsplayer` to a new package (folder) under the `src` folder.

### Useful Commands

- `python run.py run`
    Runs a game with default settings. Use `--p1`, `--p2` to use different players, and `--maps` to use different maps.
- `python run.py update`
    Update configurations for the latest version -- run this often
- `python run.py zip_submission`
    Create a submittable zip file
- `python run.py verify`
    Verify that your player `--p1` submission is valid and will be accepted
- `python run.py tasks`
    See what else you can do!


### Configuration 

Look at `properties.json` for project-wide configuration.

If you are having any problems with the default client, please report to teh devs and
feel free to set the `compatibility_client` configuration to `true` to download a different version of the client. You will also need to delete the `client_version.txt` file and run the update task to force a reinstall.
