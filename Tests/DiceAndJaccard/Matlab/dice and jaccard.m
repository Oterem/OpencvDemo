testFolder  = 'C:\Users\shail\Pictures\TestTree\test';
testList = dir(fullfile(testFolder, '*.*'));
testNfile   = length(testList);

picDir='C:\Users\shail\Pictures\TestTree';

truthFolder='C:\Users\shail\Pictures\TestTree\truth_2000';
truthList = dir(fullfile(truthFolder, '*.*'));
truthNfile   = length(truthList);

similarityList=struct('imageName',{},'dice',{},'jaccard',{});
for k=3:testNfile
    test1=""+testList(k).folder+"\"+testList(k).name;
    test1=char(test1);
    test2=""+truthList(k).folder+"\"+truthList(k).name;
    test2=char(test2)
    
    A=imread(test1);
    A=logical(A);
    
    B=imread(test2);
    B=logical(B);
    
    
     similarityList(k-2).imageName=testList(k).name;
    similarityDice = dice(A,B);
    similarityList(k-2).dice=similarityDice;
    similarityJaccard = jaccard(A,B);
    similarityList(k-2).jaccard=similarityJaccard;
%     
%  myF= figure('visible','off');
% imshowpair(A, B);
% title(['Dice Index = ' num2str(similarityDice) ' Jaccard Index= ' num2str(similarityJaccard)]);
% 
%     
%     directory=""+picDir+"\variance_"+testList(k).name;
%     directory=char(directory);
%     saveas(myF,directory);
    
    clear A;
    clear B;
%     clear myF;
    clear similarityDice;
    clear similarityJaccard;
    disp("remain: "+((testNfile)-k));
    
end

avgDice = 0;
avgJaccard = 0;


for j=1:testNfile-2
    avgDice=avgDice+similarityList(j).dice;
    avgJaccard=avgJaccard+similarityList(j).jaccard;
end

avgDice=avgDice/(testNfile-2);
avgJaccard=avgJaccard/(testNfile-2);
disp("Average Dice value: "+avgDice);
disp("Average Jaccard value: "+avgJaccard);
loc=""+picDir+"\similarityList.xlsx";
loc=char(loc);
writetable(struct2table(similarityList), loc);