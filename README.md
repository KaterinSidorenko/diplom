Source code borrowing detection system
This program is responsible for comparing the code of student papers as a pair of papers, or more, indefinitely. 
The program is connected with a FireDase : a Realtime Database, as well as a Storage in which all the students' works are divided into groups.
When you visit the page, you can select a group, laboratory, after which 3 comparison methods are provided:
1.Complete comparison.
2.Comparison without variables. 
3. Comparison by AST tree.
After comparison, a detailed report is released, which is implemented in a separate thread for stable operation of the program and display of report elements.
The report itself is made in HTML.
The distribution of laboratory work options and the creation of students is also implemented by adding them to the Firebase database

Система обнаружения студенческих работ
Данная программа отвечает за сравнене кода студенческих работ как пары работ , так и более ,до бесконечности.
Программа связана с  ФайрБейсом : РеалтаймДатабейс , а так же Сторейдж в которой хранятся все работы студентов поделённые на группы.
При заходе на страницу можно выбрать группу , лабораторные, после предоставляются 3 способа сравнения:
1.Полное сравнение.
2.Сравнение без переменных.
3.Сравнение по AST дереву .
После сравнения выходит подробный отчёт который реализован в отдельном потоке для стабильной работы программы и отображения элементов отчёта,
Сам отчёт сделан на HTML.
Так же реализовано распределение вариантов  лабораторных работ и создание студентов добавляя их в базу Файрбейса 
