import requests
import json

def get_teams():
    page = 1
    teams = []
    for i in range(1, 10):
        url = f"https://api.battlecode.org/api/team/bc25java/t/?ordering=-rating%2Cname&page={i}"
        response = requests.get(url)
        data = response.json()
        page += 1
        teams += data['results']
    return teams

json_data = get_teams()
with open('teams.json', 'w') as f:
    json.dump(json_data, f)
    