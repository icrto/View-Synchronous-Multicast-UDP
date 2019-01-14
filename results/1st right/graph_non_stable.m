clc;
clear;
nrNodes = 3;
nrStableMsgs = 3;
nrMaxNonStableMsgs = 1;
nrMeasurements = 2;

for i = 1:nrNodes-1
    filename = sprintf('%d_%d %d.csv', i, nrNodes, nrStableMsgs);
    N{i} = csvread(filename);
end

figure;
hold on;
x = (1:nrMaxNonStableMsgs+1)';
l = 1;
for i = 1:nrMaxNonStableMsgs+1
    y(i) = zeros((nrNodes-1)*nrMeasurements,1);
    l = 1;
    for j=1:nrNodes-1
        for k=1:nrMeasurements
            y(i,l) = N{j}(i, k + 1)
            l = l + 1;
        end
    end
     y(i) = [];
end
scatter(i, y, 'r', 'filled')