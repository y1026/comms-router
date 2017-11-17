(in-package :rchecker)
(defun action(name precondition step-fn update-model)
  (list precondition
        #'(lambda(model)
            (let* ((res (funcall step-fn model))
                   (new-model (if (second res)
                                  (funcall update-model
                                           res (copy-tree (jsown:extend-js model
                                                            ("result" (second res)))))
                                  model)))
              (format t "~%~A executed - ~S~%old model:~S~%new model: ~S" name res model new-model)
              (list res new-model)))
        name))

(defun del-resource(key list)
  #'(lambda(res model)
      (funcall
       (mlet ((selected (js-val key))
              (agents (js-val list)))
         (mand (js-extend
                (list list (remove-nth selected (copy-tree agents))))
               (js-remkey key)))
       model)))

(defun select-actions(index list)
  (list (action (format nil "select last of ~A" list)
                (fand (fhas-key list)
                      (fhas-kv list 0 #'(lambda(js-val val)(> (length js-val) val)))
                      (for
                       (fnot (fhas-key index))
                       (fnot (fhas-kv index 0 #'equal)) ))
                (mstep (tstep-result (format nil "Select the last of ~A." list) 0 t (format nil "Select the last of ~A." list) nil))
                #'(lambda(res model)(funcall (js-extend (list index 0)) model)))

        (action (format nil "select previous of ~A" list)
                (fand (fhas-key list)
                      (fhas-key index)
                      (fcompare-keys index list #'(lambda(selected items)(< (1+ selected) (length items)))))
                (mlet((selected (js-val index)))
                  (mstep (tstep-result (format nil "Select previous of ~A" list)  (1+ selected) t
                                 (format nil "Select previous of ~A" list) nil)) )
                #'(lambda(res model)
                    (funcall (mlet((next #'1+ (js-val index)))
                               (js-extend (list index next)))
                             model)))))

(defparameter *tasks*
  (append
   ;; ;; plans
   ;; (action "create plan"
   ;;         (fand
   ;;          (fhas-key "selected-queue")
   ;;          (for
   ;;           (fnot (fhas-key "plans"))
   ;;           (fand (fhas-kv "plans" 2 #'(lambda(js-val val)(< (length js-val) val))))))
   ;;         #'(lambda(model)(let((selected (jsown:val model "selected-queue"))
   ;;                              (queues (jsown:val model "queues"))
   ;;                              (queue (nth selected queues)))
   ;;                             (eplan-new :router-id (jsown:val model "router")
   ;;                                        :queue-id (jsown:val queue "id"))))
   ;;         #'(lambda(res model)
   ;;             (jsown:extend-js (copy-tree model)
   ;;               ("plans" (list* (jsown:new-js ("id" (jsown:val (first res) "id"))) ;;
   ;;                               (when (jsown:keyp model "plans")
   ;;                                 (jsown:val model "plans")))))))

   ;; (action "select last plan"
   ;;         (fand (fhas-key "plans")
   ;;               (fhas-kv "plans" 0 #'(lambda(js-val val)(> (length js-val) val))))
   ;;         #'(lambda(model)(tstep-result "Select the latest plan." 0 t "Select the latest plan." nil))
   ;;         #'(lambda(res model)(jsown:extend-js (copy-tree model) ("selected-plan" 0))))

   ;; (action "select next plan"
   ;;         (fand (fhas-key "plans")
   ;;               (fhas-key "selected-plan")
   ;;               (fcompare-keys "selected-plan" "plans" #'(lambda(selected items)(< (1+ selected) (length items)))))
   ;;         #'(lambda(model)(tstep-result "Select previous plan" (1+ (jsown:val model "selected-plan")) t "Select previous plan" nil))
   ;;         #'(lambda(res model)(let* ((next (1+ (jsown:val model "selected-plan"))))
   ;;                               (jsown:extend-js (copy-tree model) ("selected-plan" next)))))

   ;; (action "delete plan"
   ;;         (fand
   ;;          (fhas-key "selected-plan"))
   ;;         #'(lambda(model)(let* ((selected (jsown:val model "selected-plan"))
   ;;                                (plans (jsown:val model "plans"))
   ;;                                (plan (nth selected plans)))
   ;;                           (eplan-del :id (jsown:val plan "id") :router-id (jsown:val model "router"))))
   ;;         #'(lambda(res model)(let* ((selected (jsown:val model "selected-plan"))
   ;;                                    (plans (jsown:val model "plans"))
   ;;                                    (plan (nth selected plans)))
   ;;                               (jsown:remkey (jsown:extend-js(copy-tree model)
   ;;                                               ("plans" (remove-nth selected (copy-tree plans))))
   ;;                                             "selected-plan"))))
   (list*
    (action "create router"
            (for
             (fnot (fhas-key "routers"))
             (fhas-kv "routers" 1 #'(lambda(js-val val)(< (length js-val) val))))
            (mstep (erouter-new))
            #'(lambda(res model)
                (funcall
                 (mand
                  (js-push "routers" (jsown:new-js ("id" (jsown:val (first res) "id"))))
                  (js-extend (list "selected-router" 0)))
                 model)))
    (action "delete router"
            (fand
             (fhas-key "selected-router")
             (fnot (js-val-or "queues"))
             (fnot (js-val-or "agents"))
             )

            (mlet((router-id (js-val "id") (js-selected "selected-router" "routers")))
              (mstep (erouter-del :id router-id)))
            (del-resource "selected-router" "routers"))
    (select-actions "selected-router" "routers")
    )

   ;;queues
   (list
    (action "create queue"
            (fand
             (fhas-key "selected-router")
             (for
              (fnot (fhas-key "queues"))
              (fhas-kv "queues" 1 #'(lambda(js-val val)(< (length js-val) val)))))
            (mlet((router-id (js-val "id") (js-selected "selected-router" "routers")))
              (mstep (equeue-new :router-id router-id)))
            #'(lambda(res model)
                (funcall
                 (mand
                  (js-push "queues" (jsown:new-js ("id" (jsown:val (first res) "id"))))
                  (js-extend (list "selected-queue" 0)))
                 model)))
    (action "check queue size"
            (fand (fhas-key "selected-router")
                  (fhas-key "selected-queue") )

            (mlet((queue-id (js-val "id") (js-selected "selected-queue" "queues"))
                  (router-id (js-val "id") (js-selected "selected-router" "routers"))
                  (waiting-tasks #'length
                                 #'(lambda(tasks) (remove-if-not
                                                   (fhas-kv "state" "waiting"
                                                            #'equal)
                                                   tasks))
                                 (js-val-or "tasks")))
              (mstep (twait (equeue-size :id queue-id
                                   :description (format nil "There should be ~A tasks in the queue" waiting-tasks)
                                   :checks (check-and (has-json) (has-key "size")
                                                      (has-kv "size" waiting-tasks))
                                   :router-id router-id)
                            :timeout 3)))
            #'(lambda(res model)model))

    (action "delete queue"
            (fand
             (fhas-key "selected-router")
             (fhas-key "selected-queue")
             (fnot (js-val-or "tasks")) )

            (mlet((queue-id (js-val "id") (js-selected "selected-queue" "queues"))
                  (router-id (js-val "id") (js-selected "selected-router" "routers")))
              (mstep (equeue-del :id queue-id :router-id router-id)))
            (del-resource "selected-queue" "queues")))
   (select-actions "selected-queue" "queues")

   ;;agents
   (list (action "create agent"
                 (fand
                  (fhas-key "selected-router")
                  (for
                   (fnot (fhas-key "agents"))
                   (fand (fhas-kv "agents" 5 #'(lambda(js-val val)(< (length js-val) val))))))
           (mlet ((router-id (js-val "id") (js-selected "selected-router" "routers")))
             (mstep (eagent-new :router-id router-id)))
           #'(lambda(res model)
               (funcall
                (mand
                 (js-push "agents" (jsown:new-js ("id" (jsown:val (first res) "id"))
                                                 ("state" "offline")
                                                 ("lastTimeAtBusyState" (get-internal-real-time))))
                 (js-extend (list "selected-agent" 0)))
                model)))

   (action "set-agent ready if there are waiting tasks"
           (fand
            (fhas-key "selected-router")
            (fand (fhas-key "tasks")
                  #'(lambda(model) (some (fhas-kv "state" "waiting" #'equal)
                                         (jsown:val model"tasks"))))
            (fhas-key "selected-agent")
            (fnth "selected-agent" "agents"
                  (fnot (for
                         (fhas-kv "state" "ready" #'equal)
                         (fhas-kv "state" "busy" #'equal)))))
           (mlet((agent-id (js-val "id") (js-selected "selected-agent" "agents"))
                 (router-id (js-val "id") (js-selected "selected-router" "routers")))
             (mstep (eagent-set :id agent-id :state "ready"
                          :router-id router-id)))

           #'(lambda(res model)
               (funcall (mlet ((selected (js-val "selected-agent"))
                               (agents (js-val "agents"))
                               (tasks (js-val "tasks")))
                          (let* ((agent (nth selected agents))
                                 (task-position (position-if (fhas-kv "state" "waiting" #'equal) tasks :from-end t))
                                 (task (nth task-position tasks)))
                            (js-extend
                             (list "agents" (set-nth selected (copy-tree agents)
                                                     (jsown::extend-js agent
                                                       ("state" "busy") ) ))
                             (list "tasks" (set-nth task-position (copy-tree tasks)
                                                    (jsown::extend-js task
                                                      ("state" "assigned")
                                                      ("agent-id" (jsown:val agent "id"))))))))
                        model)))

   (action "set-agent ready if there are no waiting tasks"
           (fand
            (fhas-key "selected-router")
            (fhas-key "selected-agent")
            (fnth "selected-agent" "agents"
                  (fnot (for
                         (fhas-kv "state" "ready" #'equal)
                         (fhas-kv "state" "busy" #'equal))))
            (for
             (fnot(fhas-key "tasks"))
             (fand
              (fand (fhas-key "tasks")
                    #'(lambda(model) (not(some (fhas-kv "state" "waiting" #'equal)
                                               (jsown:val model"tasks"))))))))
           (mlet ((agent-id (js-val "id") (js-selected "selected-agent" "agents"))
                  (router-id (js-val "id") (js-selected "selected-router" "routers")))
             (mstep (eagent-set :id agent-id :state "ready"
                          :router-id router-id)))
           #'(lambda(res model)
               (funcall
                (mlet ((selected (js-val "selected-agent"))
                       (agents (js-val "agents")))
                  (let ((agent (nth selected agents)))
                    (js-extend
                     (list "agents" (set-nth selected (copy-tree agents)
                                             (jsown::extend-js agent
                                               ("state" "ready")
                                               ("lastTimeAtBusyState" (get-internal-real-time))))))))
                model)
               ))

   (action "check agent state"
           (fand (fhas-key "selected-router")
                 (fhas-key "selected-agent"))
           (mlet((agent-id (js-val "id") (js-selected "selected-agent" "agents"))
                 (agent-state (js-val "state") (js-selected "selected-agent" "agents"))
                 (router-id (js-val "id") (js-selected "selected-router" "routers")))
             (mstep
              (twait
               (eagent :id agent-id
                       :checks (has-kv "state" agent-state)
                       :router-id router-id)
               :timeout 10)))
           #'(lambda(res model)model))

   (action "delete agent"
           (fand
            (fhas-key "selected-router")
            (fhas-key "selected-agent")
            (fnth "selected-agent" "agents"
                  (fnot (fhas-kv "state" "busy" #'equal))))
           (mlet((agent-id (js-val "id") (js-selected "selected-agent" "agents"))
                 (router-id (js-val "id") (js-selected "selected-router" "routers")))
             (mstep(twait (eagent-del :id agent-id :router-id router-id)
                     :timeout 10)))
           (del-resource "selected-agent" "agents")))
   (select-actions "selected-agent" "agents")

   ;; tasks
   (list (action
          "create task when there are no ready agents"
          (fand
           (fhas-key "selected-router")
           (fhas-key "selected-queue")
           (for
            (fnot (fhas-key "tasks"))
            (fand (fhas-kv "tasks" 5 #'(lambda(js-val val)(< (length js-val) val)))))
           (for
            (fnot (fhas-key "agents"))
            (fand (fhas-key "agents") #'(lambda(model) (every #'(lambda(task)(not (equal (jsown:val task "state") "ready")))
                                                              (jsown:val model "agents"))))))
          (mlet ((router-id (js-val "id") (js-selected "selected-router" "routers"))
                 (queue-id (js-val "id") (js-selected "selected-queue" "queues"))
                 (waiting-tasks #'length
                                #'(lambda(tasks) (remove-if-not
                                                  (fhas-kv "state" "waiting"
                                                           #'equal)
                                                  tasks))
                                (js-val-or "tasks")))
            (mstep (tand
                    (twait (equeue-size :id queue-id
                                        :description (format nil "There should be ~A tasks in the queue" waiting-tasks)
                                        :checks (check-and (has-json) (has-key "size")
                                                           (has-kv "size" waiting-tasks))
                                        :router-id router-id))
                    (etask-new :checks (check-and (has-json) (has-key "id")
                                                  (has-kv "queueTasks" waiting-tasks))
                               :router-id router-id
                               :queue-id queue-id))))
          #'(lambda(res model)
              (funcall (mand (js-push "tasks" (jsown:new-js ("id" (jsown:val (first res) "id"))
                                                            ("state" "waiting")))
                             (js-extend (list "selected-task" 0)))
                       model)))

   (action
    "create task when there are ready agents"
    (fand
     (fhas-key "selected-router")
     (fhas-key "selected-queue")
     (for
      (fnot (fhas-key "tasks"))
      (fand (fhas-kv "tasks" 5 #'(lambda(js-val val)(< (length js-val) val)))))
     (fand (fhas-key "agents") #'(lambda(model) (some (fhas-kv "state" "ready" #'equal)
                                                      (jsown:val model "agents")))))
    (mlet ((router-id (js-val "id") (js-selected "selected-router" "routers"))
           (queue-id (js-val "id") (js-selected "selected-queue" "queues"))
           (agents (js-val "agents"))
           (waiting-tasks #'length
                          #'(lambda(tasks) (remove-if-not
                                            (fhas-kv "state" "waiting"
                                                     #'equal)
                                            tasks))
                          (js-val-or "tasks")))
      (mstep
       (tand
        (twait (equeue-size :id queue-id
                            :description (format nil "There should be ~A tasks in the queue" waiting-tasks)
                            :checks (check-and (has-json) (has-key "size")
                                               (has-kv "size" waiting-tasks))
                            :router-id router-id ))
        (etask-new :checks (check-and (has-json) (has-key "id")
                                      (has-kv "queueTasks" waiting-tasks))
                   :router-id router-id
                   :queue-id queue-id))))

    #'(lambda(res model)
        (funcall (mlet ((agents (js-val "agents")))
                   (let* ((agent (find-if (fhas-kv "state" "ready" #'equal)
                                          (sort  (copy-tree agents) #'<
                                                 :key (js-val "lastTimeAtBusyState"))))
                          (agent-pos (position-if (fhas-kv "id" (jsown:val agent "id") #'equal) agents)))
                     (mand
                      (js-push "tasks" (jsown:new-js ("id" (jsown:val (first res) "id"))
                                                     ("state" "assigned")
                                                     ("agent-id" (jsown:val agent "id"))))
                      (js-extend (list "agents" (set-nth agent-pos (copy-tree agents)
                                                         (jsown::extend-js agent ("state" "busy")) ))
                                 (list "selected-task" 0)) ) ) )
                 model)))

   (action
    "check task state"
    (fand (fhas-key "selected-router")
          (fhas-key "selected-task"))
    (mlet ((router-id (js-val "id") (js-selected "selected-router" "routers"))
           (task (js-selected "selected-task" "tasks")))
      (mstep (twait (etask :id (jsown:val task "id")
                     :state (jsown:val task "state")
                     :router-id router-id)
              :timeout 10)))
    #'(lambda(res model)model))

   (action
    "complete task when there are no waiting tasks"
    (fand
     (fhas-key "selected-router")
     (fhas-key "selected-task")
     (fnth "selected-task" "tasks" (fhas-kv "state" "assigned" #'equal))
     #'(lambda(model) (every (fnot (fhas-kv "state" "waiting" #'equal))
                             (jsown:val model"tasks"))))
    (mlet ((task-id (js-val "id") (js-selected "selected-task" "tasks"))
           (router-id (js-val "id") (js-selected "selected-router" "routers")))
      (mstep (etask-set :id task-id :state "completed"
                  :router-id router-id)) )
    #'(lambda(res model)
        (funcall
         (mlet((selected (js-val "selected-task"))
               (tasks (js-val "tasks"))
               (agents (js-val "agents")))
           (let*((task (nth selected tasks))
                 (agent-id (jsown:val task "agent-id"))
                 (agent-pos (position-if #'(lambda(agent) (equal (jsown:val agent "id") agent-id))
                                         agents))
                 (agent (nth agent-pos agents)))
             (js-extend (list "agents" (set-nth agent-pos (copy-tree agents)
                                                (jsown::extend-js agent
                                                  ("state" "ready")
                                                  ("lastTimeAtBusyState" (get-internal-real-time))) ))
                        (list "tasks" (set-nth selected (copy-tree tasks)
                                               (jsown::extend-js task
                                                 ("state" "completed")
                                                 ("agent-id" ())))))))
         model)))

   (action
    "complete task when there are waiting tasks"
    (fand
     (fhas-key "selected-router")
     (fhas-key "selected-task")
     (fnth "selected-task" "tasks" (fhas-kv "state" "assigned" #'equal))
     #'(lambda(model) (some (fhas-kv "state" "waiting" #'equal)
                            (jsown:val model"tasks"))))

    (mlet ((task-id (js-val "id") (js-selected "selected-task" "tasks"))
           (router-id (js-val "id") (js-selected "selected-router" "routers")))
      (mstep (etask-set :id task-id :state "completed"
                        :router-id router-id)) )
    #'(lambda(res model)
        (funcall
         (mlet ((selected (js-val "selected-task"))
                (tasks (js-val "tasks"))
                (agents (js-val "agents")) )
           (let*((task (nth selected tasks))
                 (task-position (position-if (fhas-kv "state" "waiting" #'equal) tasks :from-end t))
                 (next-task (nth task-position tasks))
                 (agent-id (jsown:val task "agent-id"))
                 (agent-pos (position-if #'(lambda(agent) (equal (jsown:val agent "id") agent-id))
                                         agents))
                 (agent (nth agent-pos agents)))
             (js-extend
              (list "agents" (set-nth agent-pos (copy-tree agents)
                                      (jsown::extend-js agent
                                        ("state" "busy")
                                        ("lastTimeAtBusyState" (get-internal-real-time)))))
              (list "tasks" (set-nth task-position
                                     (set-nth selected (copy-tree tasks)
                                              (jsown::extend-js task ("state" "completed")
                                                                ("agent-id" ())))
                                     (jsown::extend-js next-task ("state" "assigned")
                                                       ("agent-id" (jsown:val agent "id"))))) ) ))
         model)))

   (action
    "delete task"
    (fand
     (fhas-key "selected-router")
     (fhas-key "selected-task")
     (fnth "selected-task" "tasks" (for(fhas-kv "state" "completed" #'equal)
                                       (fhas-kv "state" "waiting" #'equal))) )
    (mlet ((task-id (js-val "id") (js-selected "selected-task" "tasks"))
           (router-id (js-val "id") (js-selected "selected-router" "routers")))
      (mstep (etask-del :id task-id
                  :router-id router-id)) )

    (del-resource "selected-task" "tasks")))
   (select-actions "selected-task" "tasks")
   ))
