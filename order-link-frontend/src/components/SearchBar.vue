<script setup lang="ts">
import InputText from 'primevue/inputtext';

const props = defineProps<{ modelValue: string }>();
const emit = defineEmits<{ (e: 'update:modelValue', value: string): void }>();

const onInput = (value: string | undefined) => emit('update:modelValue', value ?? '');
</script>

<template>
  <div class="controls-bar">
    <div class="p-inputgroup search-group">
    
        
  <IconField>
    <InputIcon class="pi pi-search" />
  <InputText v-model="props.modelValue" placeholder="Search by reference" size="small" 
  @update:modelValue="onInput" />
</IconField>

    </div>
  </div>
</template>