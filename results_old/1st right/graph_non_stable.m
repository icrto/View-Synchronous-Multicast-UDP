clc;
clear;
close all;
nrNodes = 3;
nrStableMsgs = 3;
nrMaxNonStableMsgs = 1;
nrMeasurements = 2;

for i = 1:nrNodes-1
    filename = sprintf('%d_%d %d.csv', i, nrNodes, nrStableMsgs);
    N{i} = csvread(filename);
end

x = (1:nrMaxNonStableMsgs+1)';
l = 1;
y = zeros(nrMaxNonStableMsgs+1, (nrNodes-1)*nrMeasurements);

fig = figure;
xlabel('Número de mensagens não estáveis', 'FontSize', 18);
ylabel('Duração da mudança de vista (ms)', 'FontSize', 18);
xticks(x')
set(gca,'XTickLabel',(x-1)');
leg = sprintf('NrMsgsEstáveis %d\nNrNós %d', nrStableMsgs, nrNodes);
title(leg);
grid on;
hold on;

for i = 1:nrMaxNonStableMsgs+1
    l = 1;
    for j=1:nrNodes-1
        for k=1:nrMeasurements
            y(i,l) = N{j}(i, k + 1)/1000;
            l = l + 1;
        end
    end
end
for i = 1:nrMaxNonStableMsgs+1
    for j=1:(nrNodes-1)*nrMeasurements
        scatter(x(i), y(i,j), 'r', 'filled')
    end
    scatter(x(i), mean(y(i,:)), 'b', 'filled')

end

figName = sprintf('NonStable %d %d.png', nrNodes, nrStableMsgs);
saveas(fig, figName);