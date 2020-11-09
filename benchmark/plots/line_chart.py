from matplotlib import pyplot as plt


def line_chart(agent_name, grakn_overviews, old_overviews, grakn_color, old_color, capsize, image_extension):

    iterations = list(int(iteration) for iteration in grakn_overviews.get(agent_name)['average'].keys())

    grakn_averages = unwrap_overviews_for_lines(agent_name, grakn_overviews, "average")
    grakn_error = unwrap_overviews_for_lines(agent_name, grakn_overviews, "standard-deviation")
    old_averages = unwrap_overviews_for_lines(agent_name, old_overviews, "average")
    old_error = unwrap_overviews_for_lines(agent_name, old_overviews, "standard-deviation")

    fig = plt.figure()
    plt.errorbar(iterations, grakn_averages, yerr=grakn_error, label='Grakn', capsize=capsize, color=grakn_color, lolims=True)
    plt.errorbar(iterations, old_averages, yerr=old_error, label='Old', capsize=capsize, color=old_color, lolims=True)

    ax = fig.axes[0]
    ax.set_ylabel('Time (ms)')
    ax.set_xlabel('Iteration')
    ax.set_title('Time Taken to Execute Agent per Iteration')
    ax.set_xticks(iterations)
    plt.legend(loc='upper left')
    plt.savefig(f'agent_{agent_name}.{image_extension}')


def unwrap_overviews_for_lines(overview_name, overviews, key):
    return overviews.get(overview_name)[key].values()
