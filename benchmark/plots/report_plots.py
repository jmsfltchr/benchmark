import collections

import numpy as np
import json
import urllib.request

from line_chart import line_chart
from overview_chart import overview_chart
import os


def get_json(json_url):
    user_agent = 'Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9.0.7) Gecko/2009021910 Firefox/3.0.7'
    request = urllib.request.Request(json_url, headers={"Authorization": f"Token {os.environ.get('GRABL_USER_TOKEN')}", 'User-Agent': user_agent})
    with urllib.request.urlopen(request) as response:
        return json.loads(response.read())


def get_trace_overviews(json_data):
    performance_analysis = json_data["performance-analysis"]
    trace_overviews = performance_analysis["trace-overviews"]
    return trace_overviews


def reformat_iterations_in_overview_metric_entry(overview_metric_entry):
    reformatted = {}
    for iteration_str, value in overview_metric_entry.items():
        reformatted[int(iteration_str)] = value
    return reformatted


def reformat_iterations_in_overviews(overviews):
    for agent_name, overview in overviews.items():
        for metric, metric_entry in overview.items():
            if metric in ["average", "standard-deviation"]:
                overview[metric] = collections.OrderedDict(sorted(reformat_iterations_in_overview_metric_entry(metric_entry).items()))
    return overviews


def get_json_url(commit_sha, analysis_id):
    return f"https://grabl.io/api/data/jmsfltchr/simulation/{commit_sha}/analysis/performance-analysis?q={{%22analysis%22:{{%22id%22:{{%22selected%22:%22{analysis_id}%22}},%22trace%22:{{%22path%22:[{{%22optional%22:true}}],%22tracker%22:{{%22optional%22:true}},%22labels%22:{{%22names%22:[]}},%22iteration%22:{{}}}}}}}}"


if __name__ == "__main__":
    old_commit_sha = "6a2761ee3efa2dfed9b62e3d90a5bb53e69f873c"
    grakn_commit_sha = "37f03c58fe328bc91496931d5171b970533d8ae8"

    # old_grakn_anaylsis_id = "7285331274130199552"
    old_anaylsis_id = "5452798450816204800"
    grakn_analysis_id = "2070020319353041920"
    # grakn_optimised_analysis_id = "695813266528300032"
    old_overviews = reformat_iterations_in_overviews(get_trace_overviews(get_json(get_json_url(old_commit_sha, old_anaylsis_id))))
    grakn_overviews = reformat_iterations_in_overviews(get_trace_overviews(get_json(get_json_url(grakn_commit_sha, grakn_analysis_id))))

    grakn_color = [113/256, 87/256, 202/256]
    old_color = [24/256, 127/256, 183/256]
    bar_edgecolor = "#000"

    agents = list(set(grakn_overviews.keys()).intersection(set(old_overviews.keys())))
    agents.remove("closeClient")
    agents.remove("closeSession")
    agents.remove("openSession")

    x = np.arange(len(agents))  # the label locations
    width = 0.3  # the width of the bars
    capsize = 3  # width of the errorbar caps

    image_extension = "png"

    # Overview charts
    overview_iterations_to_plot = [4, 8, 12]
    overview_chart(overview_iterations_to_plot, agents, x, width, capsize, bar_edgecolor, grakn_overviews, old_overviews, grakn_color, old_color, image_extension)

    # Line charts
    for agent in agents:
        line_chart(agent, grakn_overviews, old_overviews, grakn_color, old_color, capsize, image_extension)
