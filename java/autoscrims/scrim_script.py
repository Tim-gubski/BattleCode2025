import random
import time
import requests
import json
import os
from dotenv import load_dotenv
load_dotenv()



def get_teams(pages=1):
    page = 1
    teams = []
    for i in range(1, pages+1):
        url = f"https://api.battlecode.org/api/team/bc25java/t/?ordering=-rating%2Cname&page={i}"
        response = requests.get(url)
        data = response.json()
        page += 1
        teams += data['results']
    return teams

def get_maps():
    url = "https://api.battlecode.org/api/episode/bc25java/map/"
    response = requests.get(url)
    data = response.json()
    return data

def get_map_names():
    maps = get_maps()
    return [m['name'] for m in maps]

# json_data = get_teams()
# with open('teams.json', 'w') as f:
#     json.dump(json_data, f)

# url = "https://api.battlecode.org/api/token/"
# payload = {
#     "username": "timgubski",
#     "password": os.getenv('BC_PASSWORD')
# }
# response = requests.post(url, data=payload)
# token = response.json()['access']

# url = "https://api.battlecode.org/api/compete/bc25java/request/"
# payload = {"is_ranked":False,"requested_to":1335,"player_order":"?","map_names":["MoneyTower","DefaultSmall","catface"]}

if __name__ == "__main__":
    url = "https://api.battlecode.org/api/token/"
    payload = {
        "username": "timgubski",
        "password": os.getenv('BC_PASSWORD')
    }
    response = requests.post(url, data=payload)
    token = response.json()['access']

    teams = get_teams()
    print(teams)
    map_names = random.sample(get_map_names(), 9)
    print(map_names)
    srim_url = "https://api.battlecode.org/api/compete/bc25java/request/"
    for team in teams:
        payload = {"is_ranked":False,"requested_to":team['id'],"player_order":"?","map_names":map_names}
        print(payload)
        response = requests.post(srim_url, json=payload, headers={"Authorization": f"Bearer {token}"})
        print(response.json())
        time.sleep(0.1)