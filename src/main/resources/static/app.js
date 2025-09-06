// Fit4Ever Frontend Application
class Fit4EverApp {
    constructor() {
        // Use the current domain for the API URL, fallback to localhost for development
        const isDevelopment = window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1';
        this.baseURL = isDevelopment 
            ? 'http://localhost:8080/api' 
            : `${window.location.protocol}//${window.location.host}/api`;
        this.token = localStorage.getItem('fit4ever_token');
        this.user = null;
        this.currentSection = 'dashboard';
        this.editingWorkoutId = null; // Track which workout is being edited
        
        this.init();
    }

    async init() {
        this.setupEventListeners();
        this.setTodayDate();
        
        if (this.token) {
            try {
                await this.loadUserProfile();
                this.showMainApp();
                await this.loadDashboard();
            } catch (error) {
                console.error('Token validation failed:', error);
                this.logout();
            }
        } else {
            this.showAuthModal();
        }
    }

    setupEventListeners() {
        // Navigation
        document.querySelectorAll('.nav-link').forEach(link => {
            link.addEventListener('click', (e) => {
                e.preventDefault();
                const section = link.dataset.section;
                this.showSection(section);
            });
        });

        // Auth form
        document.getElementById('authForm').addEventListener('submit', this.handleAuth.bind(this));
        
        // Workout form
        document.getElementById('workoutForm').addEventListener('submit', this.handleWorkout.bind(this));
        
        // Nutrition form
        document.getElementById('nutritionForm').addEventListener('submit', this.handleNutrition.bind(this));
        
        // Goal form
        document.getElementById('goalForm').addEventListener('submit', this.handleGoal.bind(this));

        // Close modals on outside click
        document.querySelectorAll('.modal').forEach(modal => {
            modal.addEventListener('click', (e) => {
                if (e.target === modal) {
                    this.closeModal(modal.id);
                }
            });
        });
    }

    setTodayDate() {
        const today = new Date().toISOString().split('T')[0];
        document.getElementById('workoutDate').value = today;
        document.getElementById('foodDate').value = today;
        document.getElementById('nutritionDate').value = today;
        document.getElementById('startDate').value = today;
    }

    // Authentication Methods
    async handleAuth(e) {
        e.preventDefault();
        const formData = new FormData(e.target);
        const isLogin = document.querySelector('.tab-btn.active').textContent === 'Login';
        
        const endpoint = isLogin ? '/auth/login' : '/auth/register';
        const data = {
            email: formData.get('email'),
            password: formData.get('password')
        };
        
        if (!isLogin) {
            data.name = formData.get('name');
        }

        try {
            this.showLoading();
            const response = await this.apiCall(endpoint, 'POST', data, false);
            
            this.token = response.token;
            localStorage.setItem('fit4ever_token', this.token);
            
            await this.loadUserProfile();
            this.hideAuthModal();
            this.showMainApp();
            this.showToast('Authentication successful!', 'success');
            await this.loadDashboard();
        } catch (error) {
            this.showError('authError', error.message);
        } finally {
            this.hideLoading();
        }
    }

    switchAuthTab(tab) {
        const tabs = document.querySelectorAll('.tab-btn');
        const nameGroup = document.getElementById('nameGroup');
        const submitBtn = document.getElementById('authSubmitBtn');
        
        tabs.forEach(t => t.classList.remove('active'));
        document.querySelector(`[onclick="switchAuthTab('${tab}')"]`).classList.add('active');
        
        if (tab === 'register') {
            nameGroup.style.display = 'block';
            submitBtn.innerHTML = '<i class="fas fa-user-plus"></i> Register';
            document.getElementById('name').required = true;
        } else {
            nameGroup.style.display = 'none';
            submitBtn.innerHTML = '<i class="fas fa-sign-in-alt"></i> Login';
            document.getElementById('name').required = false;
        }
        
        this.clearError('authError');
    }

    async loadUserProfile() {
        try {
            this.user = await this.apiCall('/users/me', 'GET');
            document.getElementById('profileName').textContent = this.user.name;
            document.getElementById('profileEmail').textContent = this.user.email;
        } catch (error) {
            console.error('Failed to load user profile:', error);
            throw error;
        }
    }

    logout() {
        localStorage.removeItem('fit4ever_token');
        this.token = null;
        this.user = null;
        this.showAuthModal();
        this.hideMainApp();
        this.showToast('Logged out successfully', 'success');
    }

    // UI Management
    showSection(section) {
        // Update navigation
        document.querySelectorAll('.nav-link').forEach(link => {
            link.classList.remove('active');
        });
        document.querySelector(`[data-section="${section}"]`).classList.add('active');
        
        // Update content
        document.querySelectorAll('.content-section').forEach(sec => {
            sec.classList.remove('active');
        });
        document.getElementById(section).classList.add('active');
        
        this.currentSection = section;
        
        // Load section data
        this.loadSectionData(section);
    }

    async loadSectionData(section) {
        switch (section) {
            case 'dashboard':
                await this.loadDashboard();
                break;
            case 'workouts':
                await this.loadWorkouts();
                break;
            case 'nutrition':
                await this.loadNutrition();
                break;
            case 'goals':
                await this.loadGoals();
                break;
        }
    }

    // Dashboard Methods
    async loadDashboard() {
        try {
            this.showLoading();
            
            // Load stats
            const [workouts, nutrition, goals] = await Promise.all([
                this.apiCall('/workouts', 'GET'),
                this.loadTodayNutrition(),
                this.apiCall('/goals', 'GET')
            ]);
            
            // Update stats
            document.getElementById('totalWorkouts').textContent = workouts.length;
            document.getElementById('todayCalories').textContent = nutrition.totalCalories || 0;
            document.getElementById('activeGoals').textContent = goals.filter(g => g.status === 'ACTIVE').length;
            document.getElementById('weekStreak').textContent = this.calculateWeekStreak(workouts);
            
            // Load recent workouts
            this.displayRecentWorkouts(workouts.slice(0, 3));
            
            // Load goal progress
            this.displayGoalProgress(goals.slice(0, 3));
            
        } catch (error) {
            console.error('Failed to load dashboard:', error);
        } finally {
            this.hideLoading();
        }
    }

    calculateWeekStreak(workouts) {
        if (workouts.length === 0) return 0;
        
        const today = new Date();
        const oneWeekAgo = new Date(today.getTime() - 7 * 24 * 60 * 60 * 1000);
        
        return workouts.filter(w => new Date(w.date) >= oneWeekAgo).length;
    }

    displayRecentWorkouts(workouts) {
        const container = document.getElementById('recentWorkouts');
        
        if (workouts.length === 0) {
            container.innerHTML = `
                <div class="empty-state">
                    <i class="fas fa-dumbbell"></i>
                    <p>No workouts yet. Start your fitness journey!</p>
                </div>
            `;
            return;
        }
        
        container.innerHTML = workouts.map(workout => `
            <div class="recent-item">
                <div class="recent-item-info">
                    <h4>${workout.title}</h4>
                    <p>${new Date(workout.date).toLocaleDateString()}</p>
                </div>
                <div class="recent-item-meta">
                    <i class="fas fa-dumbbell"></i>
                </div>
            </div>
        `).join('');
    }

    displayGoalProgress(goals) {
        const container = document.getElementById('goalProgress');
        
        if (goals.length === 0) {
            container.innerHTML = `
                <div class="empty-state">
                    <i class="fas fa-target"></i>
                    <p>Set your first goal to track progress!</p>
                </div>
            `;
            return;
        }
        
        container.innerHTML = goals.map(goal => `
            <div class="goal-preview">
                <div class="goal-header">
                    <span class="goal-type">${goal.type.replace('_', ' ')}</span>
                    <span class="goal-status ${goal.status.toLowerCase()}">${goal.status}</span>
                </div>
                <div class="progress-bar">
                    <div class="progress-fill" style="width: ${this.calculateGoalProgress(goal)}%"></div>
                </div>
                <div class="goal-details">
                    Target: ${goal.targetValue}
                </div>
            </div>
        `).join('');
    }

    calculateGoalProgress(goal) {
        // Simple progress calculation - can be enhanced with real data
        const progress = Math.random() * 100;
        return Math.min(progress, 100);
    }

    // Workout Methods
    async loadWorkouts() {
        try {
            this.showLoading();
            const workouts = await this.apiCall('/workouts', 'GET');
            this.displayWorkouts(workouts);
        } catch (error) {
            console.error('Failed to load workouts:', error);
            this.showToast('Failed to load workouts', 'error');
        } finally {
            this.hideLoading();
        }
    }

    displayWorkouts(workouts) {
        const container = document.getElementById('workoutsGrid');
        
        if (workouts.length === 0) {
            container.innerHTML = `
                <div class="empty-state">
                    <i class="fas fa-dumbbell"></i>
                    <p>No workouts found. Create your first workout!</p>
                </div>
            `;
            return;
        }
        
        container.innerHTML = workouts.map(workout => `
            <div class="workout-card" data-id="${workout.id}">
                <div class="workout-header">
                    <div class="workout-title">${workout.title}</div>
                    <div class="workout-date">${new Date(workout.date).toLocaleDateString()}</div>
                </div>
                <div class="workout-content">
                    <p class="workout-notes">${workout.notes || 'No notes'}</p>
                    <div class="workout-exercises">
                        <h4>Exercises (${workout.exercises ? workout.exercises.length : 0})</h4>
                        ${workout.exercises && workout.exercises.length > 0 ? 
                            workout.exercises.slice(0, 3).map(exercise => `
                                <div class="exercise-preview">
                                    <span class="exercise-name">${exercise.name}</span>
                                    <span class="exercise-details">${exercise.setsCount} sets × ${exercise.repsPerSet} reps</span>
                                    ${exercise.weight ? `<span class="exercise-weight">@ ${exercise.weight}kg</span>` : ''}
                                </div>
                            `).join('') + 
                            (workout.exercises.length > 3 ? `<div class="more-exercises">+${workout.exercises.length - 3} more exercises</div>` : '')
                            : '<div class="no-exercises">No exercises added</div>'
                        }
                    </div>
                    <div class="workout-actions">
                        <button class="btn btn-sm btn-outline" onclick="app.viewWorkout(${workout.id})">
                            <i class="fas fa-edit"></i> Edit
                        </button>
                        <button class="btn btn-sm btn-danger" onclick="app.deleteWorkout(${workout.id})">
                            <i class="fas fa-trash"></i> Delete
                        </button>
                    </div>
                </div>
            </div>
        `).join('');
    }

    async viewWorkout(id) {
        try {
            const workout = await this.apiCall(`/workouts/${id}`, 'GET');
            
            // Set editing state
            this.editingWorkoutId = id;
            
            // Populate form for editing
            document.getElementById('workoutTitle').value = workout.title;
            document.getElementById('workoutDate').value = workout.date;
            document.getElementById('workoutNotes').value = workout.notes || '';
            
            // Clear existing exercises
            document.getElementById('exercisesList').innerHTML = '';
            
            // Add exercises
            if (workout.exercises && workout.exercises.length > 0) {
                workout.exercises.forEach(exercise => {
                    this.addExercise();
                    const exerciseItems = document.querySelectorAll('.exercise-item');
                    const lastItem = exerciseItems[exerciseItems.length - 1];
                    
                    lastItem.querySelector('[name="exerciseName"]').value = exercise.name;
                    lastItem.querySelector('[name="sets"]').value = exercise.setsCount;
                    lastItem.querySelector('[name="reps"]').value = exercise.repsPerSet;
                    lastItem.querySelector('[name="weight"]').value = exercise.weight || '';
                });
            } else {
                // Add one empty exercise if none exist
                this.addExercise();
            }
            
            document.getElementById('workoutModalTitle').textContent = 'Edit Workout';
            this.showModal('workoutModal');
        } catch (error) {
            this.showToast('Failed to load workout details', 'error');
        }
    }

    async deleteWorkout(id) {
        if (!confirm('Are you sure you want to delete this workout?')) return;
        
        try {
            await this.apiCall(`/workouts/${id}`, 'DELETE');
            this.showToast('Workout deleted successfully', 'success');
            await this.loadWorkouts();
        } catch (error) {
            this.showToast('Failed to delete workout', 'error');
        }
    }

    showWorkoutModal() {
        // Reset editing state for new workout
        this.editingWorkoutId = null;
        
        document.getElementById('workoutModalTitle').textContent = 'Add New Workout';
        document.getElementById('workoutForm').reset();
        document.getElementById('exercisesList').innerHTML = `
            <div class="exercise-item">
                <input type="text" placeholder="Exercise name" name="exerciseName" required>
                <input type="number" placeholder="Sets" name="sets" min="1" required>
                <input type="number" placeholder="Reps" name="reps" min="1" required>
                <input type="number" placeholder="Weight (kg)" name="weight" min="0" step="0.5">
                <button type="button" class="btn btn-danger btn-sm" onclick="removeExercise(this)">
                    <i class="fas fa-trash"></i>
                </button>
            </div>
        `;
        this.setTodayDate();
        this.showModal('workoutModal');
    }

    async handleWorkout(e) {
        e.preventDefault();
        
        const formData = new FormData(e.target);
        const exercises = this.collectExercises();
        
        if (exercises.length === 0) {
            this.showToast('Please add at least one exercise', 'error');
            return;
        }
        
        const workoutData = {
            title: formData.get('title'),
            date: formData.get('date'),
            notes: formData.get('notes'),
            exercises: exercises
        };
        
        try {
            this.showLoading();
            
            if (this.editingWorkoutId) {
                // Update existing workout
                await this.apiCall(`/workouts/${this.editingWorkoutId}`, 'PUT', workoutData);
                this.showToast('Workout updated successfully!', 'success');
            } else {
                // Create new workout
                await this.apiCall('/workouts', 'POST', workoutData);
                this.showToast('Workout created successfully!', 'success');
            }
            
            this.closeModal('workoutModal');
            this.editingWorkoutId = null; // Reset editing state
            await this.loadWorkouts();
        } catch (error) {
            this.showToast(`Failed to ${this.editingWorkoutId ? 'update' : 'create'} workout`, 'error');
        } finally {
            this.hideLoading();
        }
    }

    collectExercises() {
        const exercises = [];
        const exerciseItems = document.querySelectorAll('.exercise-item');
        
        exerciseItems.forEach(item => {
            const name = item.querySelector('[name="exerciseName"]').value;
            const sets = item.querySelector('[name="sets"]').value;
            const reps = item.querySelector('[name="reps"]').value;
            const weight = item.querySelector('[name="weight"]').value;
            
            if (name && sets && reps) {
                exercises.push({
                    name: name,
                    setsCount: parseInt(sets),
                    repsPerSet: parseInt(reps),
                    weight: weight ? parseFloat(weight) : 0
                });
            }
        });
        
        return exercises;
    }

    addExercise() {
        const container = document.getElementById('exercisesList');
        const exerciseItem = document.createElement('div');
        exerciseItem.className = 'exercise-item';
        exerciseItem.innerHTML = `
            <input type="text" placeholder="Exercise name" name="exerciseName" required>
            <input type="number" placeholder="Sets" name="sets" min="1" required>
            <input type="number" placeholder="Reps" name="reps" min="1" required>
            <input type="number" placeholder="Weight (kg)" name="weight" min="0" step="0.5">
            <button type="button" class="btn btn-danger btn-sm" onclick="removeExercise(this)">
                <i class="fas fa-trash"></i>
            </button>
        `;
        container.appendChild(exerciseItem);
    }

    removeExercise(button) {
        const exerciseItem = button.closest('.exercise-item');
        const container = document.getElementById('exercisesList');
        
        if (container.children.length > 1) {
            exerciseItem.remove();
        } else {
            this.showToast('You must have at least one exercise', 'warning');
        }
    }

    // Nutrition Methods
    async loadNutrition() {
        const date = document.getElementById('nutritionDate').value;
        await this.loadNutritionByDate(date);
    }

    async loadNutritionByDate(date = null) {
        if (!date) {
            date = document.getElementById('nutritionDate').value;
        }
        
        try {
            this.showLoading();
            const logs = await this.apiCall(`/nutrition/logs?date=${date}`, 'GET');
            const summary = await this.loadTodayNutrition(date);
            
            this.displayNutritionSummary(summary);
            this.displayNutritionLogs(logs);
        } catch (error) {
            console.error('Failed to load nutrition:', error);
        } finally {
            this.hideLoading();
        }
    }

    async loadTodayNutrition(date = null) {
        if (!date) {
            date = new Date().toISOString().split('T')[0];
        }
        
        try {
            const logs = await this.apiCall(`/nutrition/logs?date=${date}`, 'GET');
            return this.calculateNutritionTotals(logs);
        } catch (error) {
            return { totalCalories: 0, totalProtein: 0, totalCarbs: 0, totalFat: 0 };
        }
    }

    calculateNutritionTotals(logs) {
        return logs.reduce((totals, log) => {
            totals.totalCalories += log.calories;
            totals.totalProtein += log.protein;
            totals.totalCarbs += log.carbs;
            totals.totalFat += log.fat;
            return totals;
        }, { totalCalories: 0, totalProtein: 0, totalCarbs: 0, totalFat: 0 });
    }

    displayNutritionSummary(summary) {
        document.getElementById('totalCalories').textContent = Math.round(summary.totalCalories);
        document.getElementById('totalProtein').textContent = `${Math.round(summary.totalProtein)}g`;
        document.getElementById('totalCarbs').textContent = `${Math.round(summary.totalCarbs)}g`;
        document.getElementById('totalFat').textContent = `${Math.round(summary.totalFat)}g`;
    }

    displayNutritionLogs(logs) {
        const container = document.getElementById('nutritionLogs');
        
        if (logs.length === 0) {
            container.innerHTML = `
                <div class="empty-state">
                    <i class="fas fa-apple-alt"></i>
                    <p>No food logged for this date</p>
                </div>
            `;
            return;
        }
        
        container.innerHTML = logs.map(log => `
            <div class="nutrition-item">
                <div class="nutrition-item-info">
                    <h4>${log.itemName}</h4>
                    <p>${log.mealType}</p>
                </div>
                <div class="nutrition-item-stats">
                    <span>${log.calories} cal</span>
                    <span>${log.protein}g protein</span>
                    <button class="btn btn-danger btn-sm" onclick="app.deleteNutritionLog(${log.id})">
                        <i class="fas fa-trash"></i>
                    </button>
                </div>
            </div>
        `).join('');
    }

    showNutritionModal() {
        document.getElementById('nutritionForm').reset();
        this.setTodayDate();
        this.showModal('nutritionModal');
    }

    async handleNutrition(e) {
        e.preventDefault();
        
        const formData = new FormData(e.target);
        const nutritionData = {
            date: formData.get('date'),
            mealType: formData.get('mealType'),
            itemName: formData.get('itemName'),
            calories: parseInt(formData.get('calories')),
            protein: parseFloat(formData.get('protein')),
            carbs: parseFloat(formData.get('carbs')),
            fat: parseFloat(formData.get('fat'))
        };
        
        try {
            this.showLoading();
            await this.apiCall('/nutrition/logs', 'POST', nutritionData);
            this.closeModal('nutritionModal');
            this.showToast('Food logged successfully!', 'success');
            await this.loadNutrition();
        } catch (error) {
            this.showToast('Failed to log food', 'error');
        } finally {
            this.hideLoading();
        }
    }

    async deleteNutritionLog(id) {
        if (!confirm('Are you sure you want to delete this food log?')) return;
        
        try {
            await this.apiCall(`/nutrition/logs/${id}`, 'DELETE');
            this.showToast('Food log deleted successfully', 'success');
            await this.loadNutrition();
        } catch (error) {
            this.showToast('Failed to delete food log', 'error');
        }
    }

    // Goals Methods
    async loadGoals() {
        try {
            this.showLoading();
            const goals = await this.apiCall('/goals', 'GET');
            this.displayGoals(goals);
        } catch (error) {
            console.error('Failed to load goals:', error);
        } finally {
            this.hideLoading();
        }
    }

    displayGoals(goals) {
        const container = document.getElementById('goalsGrid');
        
        if (goals.length === 0) {
            container.innerHTML = `
                <div class="empty-state">
                    <i class="fas fa-target"></i>
                    <p>No goals set. Create your first goal!</p>
                </div>
            `;
            return;
        }
        
        container.innerHTML = goals.map(goal => `
            <div class="goal-card">
                <div class="goal-header">
                    <span class="goal-type">${goal.type.replace('_', ' ')}</span>
                    <span class="goal-status ${goal.status.toLowerCase()}">${goal.status}</span>
                </div>
                <div class="goal-content">
                    <div class="goal-target">
                        Target: ${goal.targetValue}
                    </div>
                    <div class="goal-progress">
                        <div class="progress-bar">
                            <div class="progress-fill" style="width: ${this.calculateGoalProgress(goal)}%"></div>
                        </div>
                    </div>
                    <div class="goal-details">
                        <small>
                            ${new Date(goal.startDate).toLocaleDateString()} - 
                            ${new Date(goal.endDate).toLocaleDateString()}
                        </small>
                    </div>
                    <div class="goal-actions">
                        <button class="btn btn-sm btn-danger" onclick="app.deleteGoal(${goal.id})">
                            <i class="fas fa-trash"></i> Delete
                        </button>
                    </div>
                </div>
            </div>
        `).join('');
    }

    showGoalModal() {
        document.getElementById('goalForm').reset();
        this.setTodayDate();
        this.showModal('goalModal');
    }

    async handleGoal(e) {
        e.preventDefault();
        
        const formData = new FormData(e.target);
        const goalData = {
            type: formData.get('type'),
            targetValue: parseFloat(formData.get('targetValue')),
            startDate: formData.get('startDate'),
            endDate: formData.get('endDate')
        };
        
        try {
            this.showLoading();
            await this.apiCall('/goals', 'POST', goalData);
            this.closeModal('goalModal');
            this.showToast('Goal created successfully!', 'success');
            await this.loadGoals();
        } catch (error) {
            this.showToast('Failed to create goal', 'error');
        } finally {
            this.hideLoading();
        }
    }

    async deleteGoal(id) {
        if (!confirm('Are you sure you want to delete this goal?')) return;
        
        try {
            await this.apiCall(`/goals/${id}`, 'DELETE');
            this.showToast('Goal deleted successfully', 'success');
            await this.loadGoals();
        } catch (error) {
            this.showToast('Failed to delete goal', 'error');
        }
    }

    // Utility Methods
    async apiCall(endpoint, method = 'GET', data = null, requireAuth = true) {
        const config = {
            method,
            headers: {
                'Content-Type': 'application/json'
            }
        };
        
        if (requireAuth && this.token) {
            config.headers['Authorization'] = `Bearer ${this.token}`;
        }
        
        if (data && method !== 'GET') {
            config.body = JSON.stringify(data);
        }
        
        const response = await fetch(`${this.baseURL}${endpoint}`, config);
        
        if (!response.ok) {
            const error = await response.text();
            throw new Error(error || `HTTP ${response.status}`);
        }
        
        if (response.status === 204) {
            return null;
        }
        
        return await response.json();
    }

    showModal(modalId) {
        document.getElementById(modalId).classList.add('active');
        document.body.style.overflow = 'hidden';
    }

    closeModal(modalId) {
        document.getElementById(modalId).classList.remove('active');
        document.body.style.overflow = 'auto';
    }

    showAuthModal() {
        this.showModal('authModal');
    }

    hideAuthModal() {
        this.closeModal('authModal');
    }

    showMainApp() {
        document.querySelector('.navbar').style.display = 'flex';
        document.querySelector('.main-content').style.display = 'block';
    }

    hideMainApp() {
        document.querySelector('.navbar').style.display = 'none';
        document.querySelector('.main-content').style.display = 'none';
    }

    showLoading() {
        document.getElementById('loadingSpinner').classList.add('active');
    }

    hideLoading() {
        document.getElementById('loadingSpinner').classList.remove('active');
    }

    showToast(message, type = 'success') {
        const container = document.getElementById('toastContainer');
        const toast = document.createElement('div');
        toast.className = `toast ${type}`;
        toast.innerHTML = `
            <div class="toast-content">
                <i class="fas fa-${type === 'success' ? 'check-circle' : type === 'error' ? 'exclamation-circle' : 'info-circle'}"></i>
                <span>${message}</span>
            </div>
        `;
        
        container.appendChild(toast);
        
        setTimeout(() => toast.classList.add('show'), 100);
        setTimeout(() => {
            toast.classList.remove('show');
            setTimeout(() => container.removeChild(toast), 300);
        }, 3000);
    }

    showError(elementId, message) {
        const errorElement = document.getElementById(elementId);
        errorElement.textContent = message;
        errorElement.style.display = 'block';
    }

    clearError(elementId) {
        const errorElement = document.getElementById(elementId);
        errorElement.textContent = '';
        errorElement.style.display = 'none';
    }
}

// Global functions for HTML onclick handlers
function switchAuthTab(tab) {
    app.switchAuthTab(tab);
}

function logout() {
    app.logout();
}

function showSection(section) {
    app.showSection(section);
}

function showWorkoutModal() {
    app.showWorkoutModal();
}

function showNutritionModal() {
    app.showNutritionModal();
}

function showGoalModal() {
    app.showGoalModal();
}

function closeModal(modalId) {
    app.closeModal(modalId);
}

function addExercise() {
    app.addExercise();
}

function removeExercise(button) {
    app.removeExercise(button);
}

function loadNutritionByDate() {
    const date = document.getElementById('nutritionDate').value;
    app.loadNutritionByDate(date);
}

function toggleNav() {
    const navMenu = document.getElementById('navMenu');
    navMenu.classList.toggle('active');
}

// Initialize app when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    window.app = new Fit4EverApp();
});
