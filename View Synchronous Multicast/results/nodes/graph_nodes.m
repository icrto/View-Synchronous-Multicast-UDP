clc;
clear;
close all;
nrMaxNodes=10;
nrMaxStableMsgs = 10;
nrMaxNonStableMsgs = 10;
nrMeasurements = 12;

filename = sprintf('final.csv', i, nrMaxNodes, nrMaxStableMsgs);
N = csvread(filename);


x = (1:nrMaxNodes-2)';
l = 1;
y = zeros(nrMaxNodes-2, nrMeasurements);

fig = figure;
xlabel('Número de nós', 'FontSize', 18);
ylabel('Duração da mudança de vista (ms)', 'FontSize', 18);
xticks(x')
set(gca,'XTickLabel',(x+2)');
leg = sprintf('NrMsgsEstáveis %d\nNrMsgsNãoEstáveis %d', nrMaxStableMsgs, nrMaxNonStableMsgs);
title(leg);
grid on;
hold on;

for i = 1:nrMaxNodes-2
    l = 1;
    for k=1:nrMeasurements
        y(i,l) = N(i, k + 1)/1000000;
        l = l + 1;
    end
end

for i = 1:nrMaxNodes-2
    for j=1:nrMeasurements
        scatter(x(i), y(i,j), 'r', 'filled')
    end
    scatter(x(i), mean(y(i,:)), 'b', 'filled')
    
end

figName = sprintf('Nodes %d %d.png', nrMaxStableMsgs, nrMaxNonStableMsgs);
saveas(fig, figName);
